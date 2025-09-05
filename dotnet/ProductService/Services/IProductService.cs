using Common.Helpers;
using ProductService.DTOs;
using ProductService.Parameters;

namespace ProductService.Services
{
    public interface IProductService
    {
        Task<ProductResponseDto> GetProductByIdAsync(Guid id, CancellationToken cancellationToken = default);
        Task<PagedList<ProductResponseDto>> GetProductsAsync(ProductParameters parameters, CancellationToken cancellationToken = default);
        Task<ProductResponseDto> CreateProductAsync(ProductCreateDto dto, CancellationToken cancellationToken = default);
        Task<ProductResponseDto> UpdateProductAsync(Guid id, ProductUpdateDto dto, CancellationToken cancellationToken = default);
        Task<bool> DeleteProductAsync(Guid id, CancellationToken cancellationToken = default);
    }
}
