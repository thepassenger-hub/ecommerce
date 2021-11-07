package it.polito.wa2.warehouseservice.services

import it.polito.wa2.warehouseservice.dto.PictureDTO
import it.polito.wa2.warehouseservice.dto.ProductDTO
import it.polito.wa2.warehouseservice.dto.WarehouseDTO
import kotlinx.coroutines.flow.Flow
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize

interface ProductService {
    suspend fun getProducts(category: String?, ids: Set<ObjectId>?, pageable: Pageable ): Flow<ProductDTO>

    suspend fun getProductById(productID: ObjectId): ProductDTO

    suspend fun getProductPicture(productID: ObjectId): PictureDTO

    @PreAuthorize("hasAuthority(\"ADMIN\")")
    suspend fun getProductWarehouses(productID: ObjectId): Flow<WarehouseDTO>

    @PreAuthorize("hasAuthority(\"ADMIN\")")
    suspend fun addProduct(productDTO: ProductDTO): ProductDTO

    @PreAuthorize("hasAuthority(\"ADMIN\")")
    suspend fun partialUpdateProduct(productDTO: ProductDTO, productID: ObjectId): ProductDTO

    @PreAuthorize("hasAuthority(\"ADMIN\")")
    suspend fun modifyProduct(productDTO: ProductDTO, productID: ObjectId): ProductDTO

    @PreAuthorize("hasAuthority(\"ADMIN\")")
    suspend fun deleteProduct(productID: ObjectId)

    @PreAuthorize("hasAuthority(\"ADMIN\")")
    suspend fun modifyProductPicture(pictureDTO: PictureDTO, productID: ObjectId): ProductDTO

    @PreAuthorize("hasAuthority(\"ADMIN\") or hasAuthority(\"CUSTOMER\")")
    suspend fun calculateRating(commentsIDs: Set<ObjectId>): Double
}