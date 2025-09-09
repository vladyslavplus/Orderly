package resources;

import dtos.AddItemRequest;
import dtos.ChangeQuantityRequest;
import dtos.CartResponse;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import security.JwtUtils;
import services.CartService;

import java.util.UUID;

@Path("api/cart")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
public class CartResource {

    @Inject
    CartService cartService;

    @Inject
    JwtUtils jwtUtils;

    @GET
    @Operation(summary = "Get current user's cart")
    public Response getCart() {
        UUID userId = jwtUtils.getUserIdFromToken();
        CartResponse cart = cartService.getCart(userId);
        return Response.ok(cart).build();
    }

    @POST
    @Path("/items")
    @RolesAllowed({"Admin", "User"})
    @Operation(summary = "Add item to cart")
    public Response addItem(@Valid AddItemRequest request) {
        UUID userId = jwtUtils.getUserIdFromToken();
        cartService.addItem(userId, request);
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/items/{productId}")
    @RolesAllowed({"Admin", "User"})
    @Operation(summary = "Remove item from cart")
    public Response removeItem(@PathParam("productId") UUID productId) {
        UUID userId = jwtUtils.getUserIdFromToken();
        cartService.removeItem(userId, productId);
        return Response.ok().build();
    }

    @PUT
    @Path("/items/{productId}/quantity")
    @RolesAllowed({"Admin", "User"})
    @Operation(summary = "Change item quantity in cart")
    public Response changeQuantity(@PathParam("productId") UUID productId,
                                   @Valid ChangeQuantityRequest request) {
        UUID userId = jwtUtils.getUserIdFromToken();
        cartService.changeItemQuantity(userId, productId, request);
        return Response.ok().build();
    }

    @DELETE
    @Path("/items/clear")
    @RolesAllowed({"Admin", "User"})
    @Operation(summary = "Clear user's cart")
    public Response clearCart() {
        UUID userId = jwtUtils.getUserIdFromToken();
        cartService.clearCart(userId);
        return Response.ok().build();
    }
}