namespace UserService.Exceptions
{
    public class UserDeletionFailedException : Exception
    {
        public UserDeletionFailedException(string errors)
            : base($"User deletion failed: {errors}") { }
    }
}
