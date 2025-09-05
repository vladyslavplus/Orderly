namespace ProductService.Exceptions
{
    public class ProductDeletionFailedException(string errors)
        : Exception($"Product deletion failed: {errors}")
    {
    }
}
