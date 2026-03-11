package ru.practicum.market.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.CartItemNotFoundException;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.integration.PaymentAdapter;
import ru.practicum.market.repository.CartItemRepository;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.service.cache.ItemCacheService;
import ru.practicum.market.service.cache.dto.CartCacheDto;
import ru.practicum.market.service.cache.dto.ItemsPageCacheDto;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.mapper.ItemMapper;
import ru.practicum.payments.integration.domain.Balance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.practicum.market.web.dto.enums.SortMethod.NO;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemServiceImpl")
class ItemServiceImplTest {

    private static final long USER_ID = TestDataFactory.USER_ID;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemCacheService itemCacheService;

    @Mock
    private PaymentAdapter paymentAdapter;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Nested
    @DisplayName("getItems")
    class GetItems {

        @Test
        @DisplayName("authenticated user gets quantities from own cart")
        void test1() {
            var itemSize = 5;
            var pageNumber = 1;
            var pageSize = 4;
            var rowSize = 3;
            var sortMethod = NO;
            String search = null;

            var items = TestDataFactory.createItems(itemSize);
            var firstItem = items.getFirst();

            var itemsCacheList = ItemMapper.toItemsCacheDto(items).subList(0, pageSize);
            var itemsPageCache = new ItemsPageCacheDto(itemsCacheList, itemSize);

            when(itemCacheService.getItemsPage(any(), any())).thenReturn(Mono.just(itemsPageCache));
            when(cartItemRepository.findByUserIdAndItemIdIn(eq(USER_ID), anyList()))
                    .thenReturn(Flux.just(TestDataFactory.createCartItem(USER_ID, firstItem.getId(), 2)));

            var response = itemService.getItems(Optional.of(USER_ID), search, sortMethod, pageNumber, pageSize).block();
            assertThat(response.items())
                    .isNotEmpty()
                    .hasSize(Math.ceilDiv(pageSize, rowSize));
            assertThat(response.items().getFirst()).hasSize(rowSize);

            assertThat(response.search()).isNull();
            assertThat(response.sort()).isEqualTo(sortMethod);

            var responseFirstItem = response.items().getFirst().getFirst();
            assertThat(responseFirstItem.id()).isEqualTo(firstItem.getId());
            assertThat(responseFirstItem.title()).isEqualTo(firstItem.getTitle());
            assertThat(responseFirstItem.description()).isEqualTo(firstItem.getDescription());
            assertThat(responseFirstItem.imgPath()).isEqualTo(firstItem.getImgPath());
            assertThat(responseFirstItem.price()).isEqualTo(firstItem.getPrice());
            assertThat(responseFirstItem.count()).isEqualTo(2);

            var responsePaging = response.paging();
            assertThat(responsePaging.pageNumber()).isEqualTo(pageNumber);
            assertThat(responsePaging.pageSize()).isEqualTo(pageSize);
            assertThat(responsePaging.hasNext()).isTrue();
            assertThat(responsePaging.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("anonymous user gets zero quantities")
        void test2() {
            var itemSize = 3;
            var pageNumber = 2;
            var pageSize = 3;
            var sortMethod = NO;
            String search = "text";

            var items = TestDataFactory.createItems(itemSize);
            var firstItem = items.getFirst();

            var itemsCacheList = ItemMapper.toItemsCacheDto(items);
            var itemsPageCache = new ItemsPageCacheDto(itemsCacheList, itemSize);

            when(itemCacheService.getItemsPage(eq(search), any())).thenReturn(Mono.just(itemsPageCache));

            var response = itemService.getItems(Optional.empty(), search, sortMethod, pageNumber, pageSize).block();
            assertThat(response.items()).hasSize(1);
            assertThat(response.search()).isEqualTo(search);
            assertThat(response.sort()).isEqualTo(sortMethod);

            var responseFirstItem = response.items().getFirst().getFirst();
            assertThat(responseFirstItem.id()).isEqualTo(firstItem.getId());
            assertThat(responseFirstItem.count()).isEqualTo(0);

            var responsePaging = response.paging();
            assertThat(responsePaging.pageNumber()).isEqualTo(pageNumber);
            assertThat(responsePaging.pageSize()).isEqualTo(itemSize);
            assertThat(responsePaging.hasNext()).isFalse();
            assertThat(responsePaging.hasPrevious()).isTrue();

            verify(cartItemRepository, never()).findByUserIdAndItemIdIn(anyLong(), anyList());
        }
    }

    @Nested
    @DisplayName("getItem")
    class GetItem {

        @Test
        @DisplayName("authenticated user")
        void test1() {
            var item = TestDataFactory.createItem(1L);
            var itemCache = ItemMapper.toItemCacheDto(item);
            var quantity = 4;
            var cartItem = new CartItem();
            cartItem.setUserId(USER_ID);
            cartItem.setItemId(item.getId());
            cartItem.setQuantity(quantity);

            when(itemCacheService.findItem(item.getId())).thenReturn(Mono.just(itemCache));
            when(cartItemRepository.findByUserIdAndItemId(USER_ID, item.getId())).thenReturn(Mono.just(cartItem));

            var response = itemService.getItem(Optional.of(USER_ID), item.getId()).block();
            assertThat(response.id()).isEqualTo(item.getId());
            assertThat(response.title()).isEqualTo(item.getTitle());
            assertThat(response.description()).isEqualTo(item.getDescription());
            assertThat(response.imgPath()).isEqualTo(item.getImgPath());
            assertThat(response.price()).isEqualTo(item.getPrice());
            assertThat(response.count()).isEqualTo(quantity);
        }

        @Test
        @DisplayName("anonymous user")
        void test2() {
            var item = TestDataFactory.createItem(1L);
            var itemCache = ItemMapper.toItemCacheDto(item);

            when(itemCacheService.findItem(item.getId())).thenReturn(Mono.just(itemCache));

            var response = itemService.getItem(Optional.empty(), item.getId()).block();
            assertThat(response.count()).isZero();

            verify(cartItemRepository, never()).findByUserIdAndItemId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("not found")
        void test3() {
            when(itemCacheService.findItem(1L)).thenReturn(
                    Mono.error(new ItemNotFoundException(1L, "Item with id = 1 not found."))
            );

            assertThatExceptionOfType(ItemNotFoundException.class)
                    .isThrownBy(() -> itemService.getItem(Optional.of(USER_ID), 1L).block());
        }
    }

    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("with items")
        void test1() {
            var items = TestDataFactory.createItems(2);
            var cartItems = List.of(
                    TestDataFactory.createCartItem(USER_ID, items.get(0).getId(), 2),
                    TestDataFactory.createCartItem(USER_ID, items.get(1).getId(), 3)
            );
            var cartCacheDto = new CartCacheDto(items.stream().map(ItemMapper::toItemCacheDto).toList());

            when(cartItemRepository.findByUserId(USER_ID)).thenReturn(Flux.fromIterable(cartItems));
            when(itemCacheService.getItemsByIds(List.of(items.get(0).getId(), items.get(1).getId())))
                    .thenReturn(Mono.just(cartCacheDto));
            when(paymentAdapter.getBalance(USER_ID)).thenReturn(Mono.just(new Balance().balance(BigDecimal.valueOf(10_000))));

            var cart = itemService.getCart(USER_ID).block();
            assertThat(cart.items()).hasSize(2);
            assertThat(cart.total()).isEqualTo(800L);
            assertThat(cart.isActiveButton()).isTrue();
        }

        @Test
        @DisplayName("empty")
        void test2() {
            when(cartItemRepository.findByUserId(USER_ID)).thenReturn(Flux.empty());

            var cart = itemService.getCart(USER_ID).block();
            assertThat(cart.items()).isEmpty();
            assertThat(cart.total()).isZero();
            assertThat(cart.isActiveButton()).isFalse();

            verify(paymentAdapter, never()).getBalance(anyLong());
        }

        @Test
        @DisplayName("insufficient funds")
        void test3() {
            var item = TestDataFactory.createItem(1L);
            var cartItem = TestDataFactory.createCartItem(USER_ID, item.getId(), 2);
            var cartCacheDto = new CartCacheDto(List.of(ItemMapper.toItemCacheDto(item)));

            when(cartItemRepository.findByUserId(USER_ID)).thenReturn(Flux.just(cartItem));
            when(itemCacheService.getItemsByIds(List.of(item.getId()))).thenReturn(Mono.just(cartCacheDto));
            when(paymentAdapter.getBalance(USER_ID)).thenReturn(Mono.just(new Balance().balance(BigDecimal.ZERO)));

            var cart = itemService.getCart(USER_ID).block();
            assertThat(cart.total()).isEqualTo(200L);
            assertThat(cart.isActiveButton()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateItemsCountInCart")
    class UpdateItemsCountInCart {

        @Test
        @DisplayName("plus")
        void test1() {
            var itemId = 1L;
            var item = TestDataFactory.createItem(itemId);
            var savedCartItem = TestDataFactory.createCartItem(USER_ID, itemId, 1);

            when(itemRepository.findById(itemId)).thenReturn(Mono.just(item));
            when(cartItemRepository.findByUserIdAndItemId(USER_ID, itemId)).thenReturn(Mono.empty());
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(savedCartItem));

            itemService.updateItemsCountInCart(USER_ID, itemId, CartAction.PLUS).block();

            ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("minus")
        void test2() {
            var itemId = 1L;
            var cartItem = TestDataFactory.createCartItem(USER_ID, itemId, 2);

            when(cartItemRepository.findByUserIdAndItemId(USER_ID, itemId)).thenReturn(Mono.just(cartItem));
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            itemService.updateItemsCountInCart(USER_ID, itemId, CartAction.MINUS).block();

            ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("minus to delete")
        void test3() {
            var itemId = 1L;
            var cartItem = TestDataFactory.createCartItem(USER_ID, itemId, 1);

            when(cartItemRepository.findByUserIdAndItemId(USER_ID, itemId)).thenReturn(Mono.just(cartItem));
            when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

            itemService.updateItemsCountInCart(USER_ID, itemId, CartAction.MINUS).block();

            verify(cartItemRepository, times(1)).delete(cartItem);
        }

        @Test
        @DisplayName("delete")
        void test4() {
            var itemId = 1L;
            var cartItem = TestDataFactory.createCartItem(USER_ID, itemId, 2);

            when(cartItemRepository.findByUserIdAndItemId(USER_ID, itemId)).thenReturn(Mono.just(cartItem));
            when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

            itemService.updateItemsCountInCart(USER_ID, itemId, CartAction.DELETE).block();

            verify(cartItemRepository, times(1)).delete(cartItem);
        }

        @Test
        @DisplayName("item not found")
        void test5() {
            when(itemRepository.findById(1L)).thenReturn(Mono.empty());

            assertThatExceptionOfType(ItemNotFoundException.class)
                    .isThrownBy(() -> itemService.updateItemsCountInCart(USER_ID, 1L, CartAction.PLUS).block());
        }

        @Test
        @DisplayName("cart item not found")
        void test6() {
            when(cartItemRepository.findByUserIdAndItemId(USER_ID, 1L)).thenReturn(Mono.empty());

            assertThatExceptionOfType(CartItemNotFoundException.class)
                    .isThrownBy(() -> itemService.updateItemsCountInCart(USER_ID, 1L, CartAction.DELETE).block());

            verify(cartItemRepository, never()).delete(any());
        }
    }
}
