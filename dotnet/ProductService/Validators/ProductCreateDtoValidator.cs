using FluentValidation;
using ProductService.DTOs;

namespace ProductService.Validators
{
    public class ProductCreateDtoValidator : AbstractValidator<ProductCreateDto>
    {
        public ProductCreateDtoValidator()
        {
            RuleFor(p => p.Name)
                .NotEmpty().WithMessage("Product name is required.")
                .MaximumLength(200).WithMessage("Product name must not exceed 200 characters.");

            RuleFor(p => p.Description)
                .MaximumLength(1000).WithMessage("Description must not exceed 1000 characters.")
                .When(p => !string.IsNullOrWhiteSpace(p.Description));

            RuleFor(p => p.Price)
                .GreaterThan(0).WithMessage("Price must be greater than zero.");

            RuleFor(p => p.Quantity)
                .GreaterThanOrEqualTo(0).WithMessage("Quantity cannot be negative.");

            RuleFor(p => p.Category)
                .MaximumLength(100).WithMessage("Category must not exceed 100 characters.")
                .When(p => !string.IsNullOrWhiteSpace(p.Category));
        }
    }
}
