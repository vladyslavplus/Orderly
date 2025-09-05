using Common.Helpers;
using Contracts.Events.Product;
using MassTransit;
using Microsoft.EntityFrameworkCore;
using ProductService.Data;
using ProductService.DTOs;
using ProductService.Exceptions;
using ProductService.Models;
using ProductService.Parameters;

namespace ProductService.Services
{
    public class ProductService : IProductService
    {
        private readonly ApplicationDbContext _context;
        private readonly ISortHelper<Product> _sortHelper;
        private readonly IPublishEndpoint _publishEndpoint;

        public ProductService(
            ApplicationDbContext context,
            ISortHelper<Product> sortHelper,
            IPublishEndpoint publishEndpoint)
        {
            _context = context;
            _sortHelper = sortHelper;
            _publishEndpoint = publishEndpoint;
        }

        public async Task<ProductResponseDto> GetProductByIdAsync(Guid id, CancellationToken cancellationToken = default)
        {
            var product = await _context.Products
                .AsNoTracking()
                .FirstOrDefaultAsync(p => p.Id == id, cancellationToken);

            if (product is null)
                throw new ProductNotFoundException(id);

            return MapToDto(product);
        }

        public async Task<PagedList<ProductResponseDto>> GetProductsAsync(ProductParameters parameters, CancellationToken cancellationToken = default)
        {
            IQueryable<Product> query = _context.Products.AsNoTracking();

            if (!string.IsNullOrWhiteSpace(parameters.Name))
                query = query.Where(p => EF.Functions.ILike(p.Name, $"%{parameters.Name}%"));

            if (!string.IsNullOrWhiteSpace(parameters.Category))
                query = query.Where(p => p.Category != null && EF.Functions.ILike(p.Category, $"%{parameters.Category}%"));

            if (parameters.MinPrice.HasValue)
                query = query.Where(p => p.Price >= parameters.MinPrice.Value);

            if (parameters.MaxPrice.HasValue)
                query = query.Where(p => p.Price <= parameters.MaxPrice.Value);

            if (parameters.MinQuantity.HasValue)
                query = query.Where(p => p.Quantity >= parameters.MinQuantity.Value);

            if (parameters.MaxQuantity.HasValue)
                query = query.Where(p => p.Quantity <= parameters.MaxQuantity.Value);

            if (parameters.MinRating.HasValue)
                query = query.Where(p => p.Rating >= parameters.MinRating.Value);

            if (parameters.MaxRating.HasValue)
                query = query.Where(p => p.Rating <= parameters.MaxRating.Value);

            query = _sortHelper.ApplySort(query, parameters.OrderBy);

            var pagedProducts = await PagedList<Product>.ToPagedListAsync(
                query,
                parameters.PageNumber,
                parameters.PageSize,
                cancellationToken);

            var dtoList = pagedProducts.Select(MapToDto).ToList();

            return new PagedList<ProductResponseDto>(
                dtoList,
                pagedProducts.TotalCount,
                pagedProducts.CurrentPage,
                pagedProducts.PageSize
            );
        }

        public async Task<ProductResponseDto> CreateProductAsync(ProductCreateDto dto, CancellationToken cancellationToken = default)
        {
            cancellationToken.ThrowIfCancellationRequested();

            var product = new Product
            {
                Id = Guid.NewGuid(),
                Name = dto.Name,
                Description = dto.Description,
                Price = dto.Price,
                Quantity = dto.Quantity,
                Category = dto.Category,
                Rating = dto.Rating ?? 0.0,
                CreatedAt = DateTime.UtcNow
            };

            await _context.Products.AddAsync(product, cancellationToken);
            await _context.SaveChangesAsync(cancellationToken);

            await _publishEndpoint.Publish(new ProductCreatedEvent(
                ProductId: product.Id,
                Name: product.Name,
                Description: product.Description,
                Price: product.Price,
                Quantity: product.Quantity,
                Category: product.Category,
                Rating: product.Rating,
                CreatedAt: product.CreatedAt
            ), cancellationToken);

            return MapToDto(product);
        }

        public async Task<ProductResponseDto> UpdateProductAsync(Guid id, ProductUpdateDto dto, CancellationToken cancellationToken = default)
        {
            var product = await _context.Products
                .FirstOrDefaultAsync(p => p.Id == id, cancellationToken);

            if (product is null)
                throw new ProductNotFoundException(id);

            bool isUpdated = UpdateProductFields(product, dto);

            if (isUpdated)
            {
                product.UpdatedAt = DateTime.UtcNow;
                await _context.SaveChangesAsync(cancellationToken);

                await _publishEndpoint.Publish(new ProductUpdatedEvent(
                    ProductId: product.Id,
                    Name: product.Name,
                    Description: product.Description,
                    Price: product.Price,
                    Quantity: product.Quantity,
                    Category: product.Category,
                    Rating: product.Rating,
                    UpdatedAt: product.UpdatedAt!.Value
                ), cancellationToken);
            }

            return MapToDto(product);
        }

        public async Task<bool> DeleteProductAsync(Guid id, CancellationToken cancellationToken = default)
        {
            var product = await _context.Products
                .FirstOrDefaultAsync(p => p.Id == id, cancellationToken);

            if (product is null)
                throw new ProductNotFoundException(id);

            var deletedAt = DateTime.UtcNow;
            var name = product.Name;
            var category = product.Category;
            var price = product.Price;
            var quantity = product.Quantity;

            _context.Products.Remove(product);
            await _context.SaveChangesAsync(cancellationToken);

            await _publishEndpoint.Publish(new ProductDeletedEvent(
                ProductId: id,
                Name: name,
                Category: category,
                Price: price,
                Quantity: quantity,
                DeletedAt: deletedAt
            ), cancellationToken);

            return true;
        }

        private static bool UpdateProductFields(Product product, ProductUpdateDto dto)
        {
            bool isUpdated = false;

            if (!string.IsNullOrWhiteSpace(dto.Name) && dto.Name != product.Name)
            {
                product.Name = dto.Name;
                isUpdated = true;
            }

            if (dto.Description is not null && dto.Description != product.Description)
            {
                product.Description = dto.Description;
                isUpdated = true;
            }

            if (dto.Price is not null && dto.Price.Value != product.Price)
            {
                product.Price = dto.Price.Value;
                isUpdated = true;
            }

            if (dto.Quantity is not null && dto.Quantity.Value != product.Quantity)
            {
                product.Quantity = dto.Quantity.Value;
                isUpdated = true;
            }

            if (dto.Category is not null && dto.Category != product.Category)
            {
                product.Category = dto.Category;
                isUpdated = true;
            }

            if (dto.Rating is not null && Math.Abs(dto.Rating.Value - product.Rating) > double.Epsilon)
            {
                product.Rating = dto.Rating.Value;
                isUpdated = true;
            }

            return isUpdated;
        }

        private static ProductResponseDto MapToDto(Product product) =>
            new ProductResponseDto
            {
                Id = product.Id,
                Name = product.Name,
                Description = product.Description,
                Price = product.Price,
                Quantity = product.Quantity,
                Category = product.Category,
                Rating = product.Rating,
                CreatedAt = product.CreatedAt,
                UpdatedAt = product.UpdatedAt
            };
    }
}
