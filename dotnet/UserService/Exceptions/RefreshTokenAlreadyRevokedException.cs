namespace UserService.Exceptions
{
    public class RefreshTokenAlreadyRevokedException : Exception
    {
        public RefreshTokenAlreadyRevokedException(string token)
            : base($"Refresh token '{token}' is already revoked.") { }
    }
}
