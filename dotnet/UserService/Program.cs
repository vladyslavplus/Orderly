using Common.Extensions;
using UserService.Data;
using UserService.Extensions;

var builder = WebApplication.CreateBuilder(args);

builder.Services
       .AddPostgresDbContext<ApplicationDbContext>(builder.Configuration)
       .AddAppIdentity(builder.Configuration)
       .AddJwtAuthentication(builder.Configuration)
       .AddCommonHelpers()
       .AddFluentValidationSetup(typeof(Program).Assembly)
       .AddUserServiceDependencies(builder.Configuration)
       .AddRabbitMqMassTransit(builder.Configuration);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerWithJwt("User Service API", "v1");

var app = builder.Build();

await UserServiceSeeder.SeedAsync(app.Services);

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();
await app.RunAsync();