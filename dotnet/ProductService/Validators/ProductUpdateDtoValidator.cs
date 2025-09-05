using FluentValidation;
using ProductService.DTOs;

namespace ProductService.Validators
{
    public class ProductUpdateDtoValidator : AbstractValidator<ProductUpdateDto>
    {
        public ProductUpdateDtoValidator()
        {
            RuleFor(p => p.Name)
                .MaximumLength(200).WithMessage("Product name must not exceed 200 characters.")
                .When(p => !string.IsNullOrWhiteSpace(p.Name));

            RuleFor(p => p.Description)
                .MaximumLength(1000).WithMessage("Description must not exceed 1000 characters.")
                .When(p => !string.IsNullOrWhiteSpace(p.Description));

            RuleFor(p => p.Price)
                .GreaterThan(0).WithMessage("Price must be greater than zero.")
                .When(p => p.Price.HasValue);

            RuleFor(p => p.Quantity)
                .GreaterThanOrEqualTo(0).WithMessage("Quantity cannot be negative.")
                .When(p => p.Quantity.HasValue);

            RuleFor(p => p.Category)
                .MaximumLength(100).WithMessage("Category must not exceed 100 characters.")
                .When(p => !string.IsNullOrWhiteSpace(p.Category));
        }
    }
}
