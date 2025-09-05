namespace ProductService.Exceptions
{
    public class ProductCreationFailedException(string errors)
        : Exception($"Product creation failed: {errors}")
    {
    }
}
