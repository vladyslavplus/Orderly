namespace ProductService.Exceptions
{
    public class ProductUpdateFailedException(string errors)
        : Exception($"Product update failed: {errors}")
    {
    }
}
