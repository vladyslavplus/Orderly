using Common.Helpers;
using UserService.DTOs;
using UserService.Models;
using UserService.Parameters;

namespace UserService.Services
{
    public interface IUserService
    {
        Task<AuthResponseDto> RegisterAsync(RegisterDto registerDto, CancellationToken cancellationToken = default);
        Task<AuthResponseDto> LoginAsync(LoginDto loginDto, CancellationToken cancellationToken = default);
        Task<UserResponseDto> GetUserByIdAsync(Guid userId, CancellationToken cancellationToken = default);
        Task<PagedList<UserResponseDto>> GetUsersAsync(UserParameters parameters, CancellationToken cancellationToken = default);
        Task<UserResponseDto> UpdateUserAsync(Guid userId, UpdateUserDto updateUserDto, CancellationToken cancellationToken = default);
        Task<bool> DeleteUserAsync(Guid userId, CancellationToken cancellationToken = default);
        Task<string> GenerateRefreshTokenAsync(Guid userId, CancellationToken cancellationToken = default);
        Task<AuthResponseDto> RefreshTokenAsync(string token, CancellationToken cancellationToken = default);
        Task<bool> RevokeRefreshTokenAsync(string token, CancellationToken cancellationToken = default);
    }
}