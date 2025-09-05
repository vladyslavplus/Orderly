namespace ProductService.Exceptions
{
    public class ProductNotFoundException(Guid productId) : Exception($"Product with Id '{productId}' was not found.")
    {
    }
}
