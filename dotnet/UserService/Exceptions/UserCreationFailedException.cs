namespace UserService.Exceptions
{
    public class UserCreationFailedException(string errors) : Exception($"User creation failed: {errors}")
    {
    }
}
