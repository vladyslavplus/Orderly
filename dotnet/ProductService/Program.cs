using Common.Extensions;
using ProductService.Data;
using ProductService.Extensions;

var builder = WebApplication.CreateBuilder(args);

builder.Services
       .AddPostgresDbContext<ApplicationDbContext>(builder.Configuration)
       .AddJwtAuthentication(builder.Configuration)
       .AddCommonHelpers()
       .AddFluentValidationSetup(typeof(Program).Assembly)
       .AddProductServiceDependencies(builder.Configuration)
       .AddRabbitMqMassTransit(builder.Configuration);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerWithJwt("Product Service API", "v1");

var app = builder.Build();

app.UseDatabaseMigration<ApplicationDbContext>();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseProductExceptionHandling();
app.UseGlobalExceptionHandling();

app.UseHttpsRedirection();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();
await app.RunAsync();
