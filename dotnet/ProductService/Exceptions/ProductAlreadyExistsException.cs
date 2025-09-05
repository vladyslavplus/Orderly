namespace ProductService.Exceptions
{
    public class ProductAlreadyExistsException(string name)
        : Exception($"Product with name '{name}' already exists.")
    {
    }
}
