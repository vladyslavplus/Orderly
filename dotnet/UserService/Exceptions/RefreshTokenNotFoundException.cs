namespace UserService.Exceptions
{
    public class RefreshTokenNotFoundException : Exception
    {
        public RefreshTokenNotFoundException(string token)
            : base($"Refresh token '{token}' not found.") { }
    }
}
