using Common.Helpers;
using Contracts.Events.User;
using MassTransit;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using UserService.DTOs;
using UserService.Exceptions;
using UserService.Models;
using UserService.Parameters;

namespace UserService.Services
{
    public class UserService : IUserService
    {
        private readonly UserManager<ApplicationUser> _userManager;
        private readonly SignInManager<ApplicationUser> _signInManager;
        private readonly IConfiguration _configuration;
        private readonly IPublishEndpoint _publishEndpoint;
        private readonly ISortHelper<ApplicationUser> _sortHelper;

        public UserService(
            UserManager<ApplicationUser> userManager,
            SignInManager<ApplicationUser> signInManager,
            IConfiguration configuration,
            IPublishEndpoint publishEndpoint,
            ISortHelper<ApplicationUser> sortHelper)   
        {
            _userManager = userManager;
            _signInManager = signInManager;
            _configuration = configuration;
            _publishEndpoint = publishEndpoint;
            _sortHelper = sortHelper;
        }

        public async Task<AuthResponseDto> RegisterAsync(RegisterDto registerDto, CancellationToken cancellationToken = default)
        {
            var existingUser = await _userManager.FindByEmailAsync(registerDto.Email);
            if (existingUser is not null)
                throw new UserAlreadyExistsException(registerDto.Email);

            var user = new ApplicationUser
            {
                UserName = registerDto.UserName,
                Email = registerDto.Email
            };

            var result = await _userManager.CreateAsync(user, registerDto.Password);
            if (!result.Succeeded)
            {
                var errors = string.Join("; ", result.Errors.Select(e => e.Description));
                throw new UserCreationFailedException(errors);
            }

            var refreshToken = await GenerateRefreshTokenAsync(user.Id, cancellationToken);
            var accessToken = await GenerateJwtTokenAsync(user);

            var userCreatedEvent = new UserCreatedEvent(
                UserId: user.Id,
                UserName: user.UserName!,
                Email: user.Email!,
                CreatedAt: DateTime.UtcNow
            );

            await _publishEndpoint.Publish(userCreatedEvent, cancellationToken);

            return new AuthResponseDto
            {
                AccessToken = accessToken,
                RefreshToken = refreshToken
            };
        }

        public async Task<AuthResponseDto> LoginAsync(LoginDto loginDto, CancellationToken cancellationToken = default)
        {
            var user = await _userManager.FindByEmailAsync(loginDto.Email);
            if (user is null)
                throw new InvalidCredentialsException();

            var result = await _signInManager.CheckPasswordSignInAsync(user, loginDto.Password, false);
            if (!result.Succeeded)
                throw new InvalidCredentialsException();

            var refreshToken = await GenerateRefreshTokenAsync(user.Id, cancellationToken);
            var accessToken = await GenerateJwtTokenAsync(user);

            return new AuthResponseDto
            {
                AccessToken = accessToken,
                RefreshToken = refreshToken
            };
        }

        public async Task<UserResponseDto> GetUserByIdAsync(Guid userId, CancellationToken cancellationToken = default)
        {
            var user = await _userManager.Users
                .AsNoTracking()
                .FirstOrDefaultAsync(u => u.Id == userId, cancellationToken);

            if (user is null)
                throw new UserNotFoundException(userId);

            return MapToDto(user);
        }

        public async Task<PagedList<UserResponseDto>> GetUsersAsync(UserParameters parameters, CancellationToken cancellationToken = default)
        {
            IQueryable<ApplicationUser> query = _userManager.Users.AsNoTracking();

            if (!string.IsNullOrWhiteSpace(parameters.UserName))
                query = query.Where(u => EF.Functions.ILike(u.UserName!, $"%{parameters.UserName}%"));

            if (!string.IsNullOrWhiteSpace(parameters.Email))
                query = query.Where(u => EF.Functions.ILike(u.Email!, $"%{parameters.Email}%"));

            if (!string.IsNullOrWhiteSpace(parameters.PhoneNumber))
                query = query.Where(u => EF.Functions.ILike(u.PhoneNumber!, $"%{parameters.PhoneNumber}%"));

            if (parameters.CreatedAtFrom.HasValue)
                query = query.Where(u => u.CreatedAt >= parameters.CreatedAtFrom.Value);

            if (parameters.CreatedAtTo.HasValue)
                query = query.Where(u => u.CreatedAt <= parameters.CreatedAtTo.Value);

            if (parameters.EmailConfirmed.HasValue)
                query = query.Where(u => u.EmailConfirmed == parameters.EmailConfirmed.Value);

            if (parameters.PhoneNumberConfirmed.HasValue)
                query = query.Where(u => u.PhoneNumberConfirmed == parameters.PhoneNumberConfirmed.Value);

            query = _sortHelper.ApplySort(query, parameters.OrderBy);

            var pagedUsers = await PagedList<ApplicationUser>.ToPagedListAsync(
                query,
                parameters.PageNumber,
                parameters.PageSize,
                cancellationToken);

            var pagedDto = new PagedList<UserResponseDto>(
                pagedUsers.Select(MapToDto).ToList(),
                pagedUsers.TotalCount,
                pagedUsers.CurrentPage,
                pagedUsers.PageSize
            );

            return pagedDto;
        }

        public async Task<UserResponseDto> UpdateUserAsync(Guid userId, UpdateUserDto updateUserDto, CancellationToken cancellationToken = default)
        {
            var user = await _userManager.Users
                .Include(u => u.RefreshTokens)
                .FirstOrDefaultAsync(u => u.Id == userId, cancellationToken);

            if (user is null)
                throw new UserNotFoundException(userId);

            bool isUpdated = false;

            if (!string.IsNullOrWhiteSpace(updateUserDto.UserName) && updateUserDto.UserName != user.UserName)
            {
                user.UserName = updateUserDto.UserName;
                isUpdated = true;
            }

            if (!string.IsNullOrWhiteSpace(updateUserDto.Email) && updateUserDto.Email != user.Email)
            {
                user.Email = updateUserDto.Email;
                isUpdated = true;
            }

            if (!string.IsNullOrWhiteSpace(updateUserDto.PhoneNumber) && updateUserDto.PhoneNumber != user.PhoneNumber)
            {
                user.PhoneNumber = updateUserDto.PhoneNumber;
                isUpdated = true;
            }

            if (!string.IsNullOrWhiteSpace(updateUserDto.Password))
            {
                var token = await _userManager.GeneratePasswordResetTokenAsync(user);
                var result = await _userManager.ResetPasswordAsync(user, token, updateUserDto.Password);
                if (!result.Succeeded)
                {
                    var errors = string.Join("; ", result.Errors.Select(e => e.Description));
                    throw new UserUpdateFailedException(errors);
                }
                isUpdated = true;
            }

            if (isUpdated)
            {
                user.UpdatedAt = DateTime.UtcNow;
                var updateResult = await _userManager.UpdateAsync(user);
                if (!updateResult.Succeeded)
                {
                    var errors = string.Join("; ", updateResult.Errors.Select(e => e.Description));
                    throw new UserUpdateFailedException(errors);
                }

                var userUpdatedEvent = new UserUpdatedEvent(
                    UserId: user.Id,
                    UserName: user.UserName,
                    Email: user.Email,
                    PhoneNumber: user.PhoneNumber,
                    UpdatedAt: user.UpdatedAt
                );

                await _publishEndpoint.Publish(userUpdatedEvent, cancellationToken);
            }

            return MapToDto(user);
        }

        public async Task<bool> DeleteUserAsync(Guid userId, CancellationToken cancellationToken = default)
        {
            var user = await _userManager.Users
                .Include(u => u.RefreshTokens)
                .FirstOrDefaultAsync(u => u.Id == userId, cancellationToken);

            if (user is null)
                throw new UserNotFoundException(userId);

            var result = await _userManager.DeleteAsync(user);
            if (!result.Succeeded)
            {
                var errors = string.Join("; ", result.Errors.Select(e => e.Description));
                throw new UserDeletionFailedException(errors);
            }

            var userDeletedEvent = new UserDeletedEvent(
                UserId: user.Id,
                Email: user.Email!,
                DeletedAt: DateTime.UtcNow
            );

            await _publishEndpoint.Publish(userDeletedEvent, cancellationToken);

            return true;
        }

        private async Task<string> GenerateJwtTokenAsync(ApplicationUser user)
        {
            var authClaims = new List<Claim>
            {
                new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()),
                new Claim(ClaimTypes.Name, user.UserName ?? string.Empty),
                new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
            };

            var userRoles = await _userManager.GetRolesAsync(user);
            foreach (var role in userRoles)
                authClaims.Add(new Claim(ClaimTypes.Role, role));

            var authSigningKey = new SymmetricSecurityKey(
                Encoding.UTF8.GetBytes(_configuration["Jwt:Key"]!));

            var token = new JwtSecurityToken(
                issuer: _configuration["Jwt:Issuer"],
                audience: _configuration["Jwt:Audience"],
                expires: DateTime.UtcNow.AddMinutes(double.Parse(_configuration["Jwt:AccessTokenExpirationMinutes"] ?? "60")),
                claims: authClaims,
                signingCredentials: new SigningCredentials(authSigningKey, SecurityAlgorithms.HmacSha256)
            );

            return await Task.FromResult(new JwtSecurityTokenHandler().WriteToken(token));
        }

        public async Task<string> GenerateRefreshTokenAsync(Guid userId, CancellationToken cancellationToken = default)
        {
            var user = await _userManager.Users
                                         .Include(u => u.RefreshTokens)
                                         .FirstOrDefaultAsync(u => u.Id == userId, cancellationToken);
            if (user is null)
                throw new UserNotFoundException(userId);

            var token = Convert.ToBase64String(RandomNumberGenerator.GetBytes(64));

            var refreshToken = new RefreshToken
            {
                Token = token,
                Expires = DateTime.UtcNow.AddDays(7),
                UserId = user.Id
            };

            user.RefreshTokens.Add(refreshToken);

            var result = await _userManager.UpdateAsync(user);
            if (!result.Succeeded)
            {
                var errors = string.Join("; ", result.Errors.Select(e => e.Description));
                throw new Exception($"Failed to save refresh token: {errors}");
            }

            return token;
        }

        public async Task<AuthResponseDto> RefreshTokenAsync(string token, CancellationToken cancellationToken = default)
        {
            if (string.IsNullOrWhiteSpace(token))
                throw new InvalidRefreshTokenException("Token is null or empty.");

            var user = await _userManager.Users
                .Include(u => u.RefreshTokens)
                .FirstOrDefaultAsync(u => u.RefreshTokens.Any(t => t.Token == token), cancellationToken);

            if (user is null)
                throw new UserNotFoundException(Guid.Empty); 

            var refreshToken = user.RefreshTokens.FirstOrDefault(t => t.Token == token);

            if (refreshToken is null || refreshToken.IsRevoked || refreshToken.Expires < DateTime.UtcNow)
                throw new InvalidRefreshTokenException("Refresh token is invalid, revoked, or expired.");

            refreshToken.IsRevoked = true;
            refreshToken.Revoked = DateTime.UtcNow;

            var newRefreshToken = await GenerateRefreshTokenAsync(user.Id, cancellationToken);
            var accessToken = await GenerateJwtTokenAsync(user);

            await _userManager.UpdateAsync(user);

            return new AuthResponseDto
            {
                AccessToken = accessToken,
                RefreshToken = newRefreshToken
            };
        }

        public async Task<bool> RevokeRefreshTokenAsync(string token, CancellationToken cancellationToken = default)
        {
            var refreshToken = await _userManager.Users
                .Include(u => u.RefreshTokens)
                .SelectMany(u => u.RefreshTokens)
                .FirstOrDefaultAsync(rt => rt.Token == token, cancellationToken);

            if (refreshToken is null)
                throw new RefreshTokenNotFoundException(token);

            if (refreshToken.IsRevoked)
                throw new RefreshTokenAlreadyRevokedException(token);

            refreshToken.IsRevoked = true;
            refreshToken.Revoked = DateTime.UtcNow;

            var user = await _userManager.Users
                .Include(u => u.RefreshTokens)
                .FirstOrDefaultAsync(u => u.RefreshTokens.Any(t => t.Token == token), cancellationToken);

            if (user is null)
                throw new UserNotFoundException(refreshToken.UserId);

            var result = await _userManager.UpdateAsync(user);
            if (!result.Succeeded)
            {
                var errors = string.Join("; ", result.Errors.Select(e => e.Description));
                throw new Exception($"Failed to revoke refresh token: {errors}");
            }

            return true;
        }

        private UserResponseDto MapToDto(ApplicationUser user)
        {
            return new UserResponseDto
            {
                Id = user.Id,
                UserName = user.UserName!,
                Email = user.Email!,
                PhoneNumber = user.PhoneNumber,
                EmailConfirmed = user.EmailConfirmed,
                PhoneNumberConfirmed = user.PhoneNumberConfirmed,
                CreatedAt = user.CreatedAt,
                UpdatedAt = user.UpdatedAt
            };
        }
    }
}
