using Microsoft.AspNetCore.Builder;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace Common.Extensions
{
    public static class DbContextExtensions
    {
        public static IServiceCollection AddPostgresDbContext<TContext>(
            this IServiceCollection services,
            IConfiguration configuration,
            string connectionStringName = "DefaultConnection")
            where TContext : DbContext
        {
            var connectionString = configuration.GetConnectionString(connectionStringName);
            services.AddDbContext<TContext>(options =>
                options.UseNpgsql(connectionString));

            return services;
        }

        public static IApplicationBuilder UseDatabaseMigration<TContext>(
            this IApplicationBuilder app)
            where TContext : DbContext
        {
            using var scope = app.ApplicationServices.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<TContext>();
            db.Database.Migrate();
            return app;
        }
    }
}
