using FluentValidation;
using UserService.DTOs;

namespace UserService.Validators
{
    public class UpdateUserDtoValidator : AbstractValidator<UpdateUserDto>
    {
        public UpdateUserDtoValidator()
        {
            When(x => x.UserName != null, () =>
            {
                RuleFor(x => x.UserName)
                    .MinimumLength(3).WithMessage("UserName must be at least 3 characters.");
            });

            When(x => x.Email != null, () =>
            {
                RuleFor(x => x.Email)
                    .EmailAddress().WithMessage("Invalid email format.");
            });

            When(x => x.Password != null, () =>
            {
                RuleFor(x => x.Password)
                    .MinimumLength(6).WithMessage("Password must be at least 6 characters.");
            });

            When(x => x.PhoneNumber != null, () =>
            {
                RuleFor(x => x.PhoneNumber)
                    .Matches(@"^\+?\d{7,15}$").WithMessage("PhoneNumber must be a valid number.");
            });
        }
    }
}
