namespace UserService.Exceptions
{
    public class UserUpdateFailedException : Exception
    {
        public UserUpdateFailedException(string errors)
            : base($"User update failed: {errors}") { }
    }
}
