using Microsoft.AspNetCore.Identity;
using System.Text.Json.Serialization;

namespace UserService.Models
{
    public class ApplicationUser : IdentityUser<Guid>
    {
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
        public DateTime UpdatedAt { get; set; }
        [JsonIgnore]
        public List<RefreshToken> RefreshTokens { get; set; } = new();
    }
}
