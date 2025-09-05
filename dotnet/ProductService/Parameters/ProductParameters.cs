using Common.Parameters;

namespace ProductService.Parameters
{
    public class ProductParameters : QueryStringParameters
    {
        public string? Name { get; set; }
        public string? Category { get; set; }
        public decimal? MinPrice { get; set; }
        public decimal? MaxPrice { get; set; }
        public int? MinQuantity { get; set; }   
        public int? MaxQuantity { get; set; }
        public double? MinRating { get; set; }
        public double? MaxRating { get; set; }
    }
}
