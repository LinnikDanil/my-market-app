package ru.practicum.market.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.practicum.market.domain.exception.CartItemNotFoundException;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.repository.CartItemRepository;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.dto.enums.CartAction;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.practicum.market.web.dto.enums.SortMethod.NO;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemServiceImpl")
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Nested
    @DisplayName("getItems")
    class getItems {

        @Test
        @DisplayName("search null")
        void test1() {
            var itemSize = 5;
            var pageNumber = 1;
            var pageSize = 5;
            var rowSize = 3;
            var sortMethod = NO;
            String search = null;

            var items = TestDataFactory.createItems(itemSize);
            var firstItem = items.getFirst();

            when(itemRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.fromIterable(items));
            when(itemRepository.count()).thenReturn(Mono.just(10L));
            when(cartItemRepository.findByItemIdIn(anyList()))
                    .thenReturn(Flux.just(TestDataFactory.createCartItem(firstItem.getId(), 2)));

            var response = itemService.getItems(search, sortMethod, pageNumber, pageSize).block();
            assertThat(response.items())
                    .isNotEmpty()
                    .hasSize(Math.ceilDiv(itemSize, rowSize));
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
            assertThat(responsePaging.pageSize()).isEqualTo(itemSize);
            assertThat(responsePaging.hasNext()).isTrue();
            assertThat(responsePaging.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("search not null")
        void test2() {
            var itemSize = 3;
            var pageNumber = 2;
            var pageSize = 3;
            var sortMethod = NO;
            String search = "text";

            var items = TestDataFactory.createItems(itemSize);
            var firstItem = items.getFirst();

            when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    any(), any(), any(Pageable.class)))
                    .thenReturn(Flux.fromIterable(items));
            when(itemRepository.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(any(), any()))
                    .thenReturn(Mono.just((long) itemSize));
            when(cartItemRepository.findByItemIdIn(anyList())).thenReturn(Flux.empty());

            var response = itemService.getItems(search, sortMethod, pageNumber, pageSize).block();
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
        }
    }

    @Nested
    @DisplayName("getItem")
    class getItem {

        @Test
        @DisplayName("ok")
        void test1() {
            var item = TestDataFactory.createItem(1L);
            var quantity = 4;
            var cartItem = new CartItem();
            cartItem.setItemId(item.getId());
            cartItem.setQuantity(quantity);

            when(itemRepository.findById(item.getId())).thenReturn(Mono.just(item));
            when(cartItemRepository.findByItemId(item.getId())).thenReturn(Mono.just(cartItem));

            var response = itemService.getItem(item.getId()).block();
            assertThat(response.id()).isEqualTo(item.getId());
            assertThat(response.title()).isEqualTo(item.getTitle());
            assertThat(response.description()).isEqualTo(item.getDescription());
            assertThat(response.imgPath()).isEqualTo(item.getImgPath());
            assertThat(response.price()).isEqualTo(item.getPrice());
            assertThat(response.count()).isEqualTo(quantity);
        }

        @Test
        @DisplayName("not found")
        void test2() {
            when(itemRepository.findById(1L)).thenReturn(Mono.empty());

            assertThatExceptionOfType(ItemNotFoundException.class)
                    .isThrownBy(() -> itemService.getItem(1L).block());
        }
    }

    @Nested
    @DisplayName("getCart")
    class getCart {

        @Test
        @DisplayName("with items")
        void test1() {
            var items = TestDataFactory.createItems(2);
            var cartItems = List.of(
                    TestDataFactory.createCartItem(items.get(0).getId(), 2),
                    TestDataFactory.createCartItem(items.get(1).getId(), 3)
            );

            when(cartItemRepository.findAll()).thenReturn(Flux.fromIterable(cartItems));
            when(itemRepository.findByIdIn(List.of(items.get(0).getId(), items.get(1).getId())))
                    .thenReturn(Flux.fromIterable(items));

            var cart = itemService.getCart().block();
            assertThat(cart.items()).hasSize(2);
            assertThat(cart.total()).isEqualTo(800L);
        }

        @Test
        @DisplayName("empty")
        void test2() {
            when(cartItemRepository.findAll()).thenReturn(Flux.empty());

            var cart = itemService.getCart().block();
            assertThat(cart.items()).isEmpty();
            assertThat(cart.total()).isZero();
        }
    }

    @Nested
    @DisplayName("updateItemsCountInCart")
    class updateItemsCountInCart {

        @Test
        @DisplayName("plus")
        void test1() {
            var itemId = 1L;
            var item = TestDataFactory.createItem(itemId);
            var savedCartItem = TestDataFactory.createCartItem(itemId, 1);

            when(itemRepository.findById(itemId)).thenReturn(Mono.just(item));
            when(cartItemRepository.findByItemId(itemId)).thenReturn(Mono.empty());
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(savedCartItem));

            itemService.updateItemsCountInCart(itemId, CartAction.PLUS).block();

            ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("minus")
        void test2() {
            var itemId = 1L;
            var cartItem = TestDataFactory.createCartItem(itemId, 2);

            when(cartItemRepository.findByItemId(itemId)).thenReturn(Mono.just(cartItem));
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            itemService.updateItemsCountInCart(itemId, CartAction.MINUS).block();

            ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("minus to delete")
        void test3() {
            var itemId = 1L;
            var cartItem = TestDataFactory.createCartItem(itemId, 1);

            when(cartItemRepository.findByItemId(itemId)).thenReturn(Mono.just(cartItem));
            when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

            itemService.updateItemsCountInCart(itemId, CartAction.MINUS).block();

            verify(cartItemRepository, times(1)).delete(cartItem);
        }

        @Test
        @DisplayName("delete")
        void test4() {
            var itemId = 1L;
            var cartItem = TestDataFactory.createCartItem(itemId, 2);

            when(cartItemRepository.findByItemId(itemId)).thenReturn(Mono.just(cartItem));
            when(cartItemRepository.delete(cartItem)).thenReturn(Mono.empty());

            itemService.updateItemsCountInCart(itemId, CartAction.DELETE).block();

            verify(cartItemRepository, times(1)).delete(cartItem);
        }

        @Test
        @DisplayName("item not found")
        void test5() {
            when(itemRepository.findById(1L)).thenReturn(Mono.empty());

            assertThatExceptionOfType(ItemNotFoundException.class)
                    .isThrownBy(() -> itemService.updateItemsCountInCart(1L, CartAction.PLUS).block());
        }

        @Test
        @DisplayName("cart item not found")
        void test6() {
            when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.empty());

            assertThatExceptionOfType(CartItemNotFoundException.class)
                    .isThrownBy(() -> itemService.updateItemsCountInCart(1L, CartAction.DELETE).block());

            verify(cartItemRepository, never()).delete(any());
        }
    }
}
