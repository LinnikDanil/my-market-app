package ru.practicum.market.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.market.domain.exception.CartItemNotFoundException;
import ru.practicum.market.domain.exception.ItemNotFoundException;
import ru.practicum.market.domain.model.CartItem;
import ru.practicum.market.domain.model.Item;
import ru.practicum.market.repository.CartItemRepository;
import ru.practicum.market.repository.ItemRepository;
import ru.practicum.market.util.TestDataFactory;
import ru.practicum.market.web.dto.enums.CartAction;
import ru.practicum.market.web.dto.enums.SortMethod;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.practicum.market.web.dto.enums.SortMethod.ALPHA;
import static ru.practicum.market.web.dto.enums.SortMethod.NO;
import static ru.practicum.market.web.dto.enums.SortMethod.PRICE;

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

            var pageable = PageRequest.of(pageNumber - 1, pageSize, getSortMethod(sortMethod));
            var itemsPage = TestDataFactory.createItemPage(itemSize, 10, pageable);
            var firstItem = itemsPage.getContent().getFirst();
            when(itemRepository.findAll(any(Pageable.class))).thenReturn(itemsPage);
            when(cartItemRepository.findByItemIds(anyList())).thenReturn(Collections.emptyList());

            var response = itemService.getItems(search, sortMethod, pageNumber, pageSize);

            assertThat(response).isNotNull();

            assertThat(response.items())
                    .isNotEmpty()
                    .hasSize(Math.ceilDiv(itemSize, rowSize));
            assertThat(response.items().getFirst().size()).isEqualTo(rowSize);

            assertThat(response.search()).isNull();
            assertThat(response.sort()).isEqualTo(sortMethod);

            var responseFirstItem = response.items().getFirst().getFirst();
            assertThat(responseFirstItem.id()).isEqualTo(firstItem.getId());
            assertThat(responseFirstItem.title()).isEqualTo(firstItem.getTitle());
            assertThat(responseFirstItem.description()).isEqualTo(firstItem.getDescription());
            assertThat(responseFirstItem.imgPath()).isEqualTo(firstItem.getImgPath());
            assertThat(responseFirstItem.price()).isEqualTo(firstItem.getPrice());
            assertThat(responseFirstItem.count()).isEqualTo(0);

            var responsePaging = response.paging();
            assertThat(responsePaging.pageNumber()).isEqualTo(pageNumber);
            assertThat(responsePaging.pageSize()).isEqualTo(pageSize);
            assertThat(responsePaging.hasNext()).isTrue();
            assertThat(responsePaging.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("search not null")
        void test2() {
            var itemSize = 5;
            var pageNumber = 1;
            var pageSize = 5;
            var rowSize = 3;
            var sortMethod = NO;
            String search = "text";

            var pageable = PageRequest.of(pageNumber - 1, pageSize, getSortMethod(sortMethod));
            var itemsPage = TestDataFactory.createItemPage(itemSize, 10, pageable);
            var firstItem = itemsPage.getContent().getFirst();
            when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    eq(search), eq(search), any(Pageable.class))
            ).thenReturn(itemsPage);
            when(cartItemRepository.findByItemIds(anyList())).thenReturn(Collections.emptyList());

            var response = itemService.getItems(search, sortMethod, pageNumber, pageSize);

            assertThat(response).isNotNull();

            assertThat(response.items())
                    .isNotEmpty()
                    .hasSize(Math.ceilDiv(itemSize, rowSize));
            assertThat(response.items().getFirst().size()).isEqualTo(rowSize);

            assertThat(response.search()).isEqualTo(search);
            assertThat(response.sort()).isEqualTo(sortMethod);

            var responseFirstItem = response.items().getFirst().getFirst();
            assertThat(responseFirstItem.id()).isEqualTo(firstItem.getId());
            assertThat(responseFirstItem.title()).isEqualTo(firstItem.getTitle());
            assertThat(responseFirstItem.description()).isEqualTo(firstItem.getDescription());
            assertThat(responseFirstItem.imgPath()).isEqualTo(firstItem.getImgPath());
            assertThat(responseFirstItem.price()).isEqualTo(firstItem.getPrice());
            assertThat(responseFirstItem.count()).isEqualTo(0);

            var responsePaging = response.paging();
            assertThat(responsePaging.pageNumber()).isEqualTo(pageNumber);
            assertThat(responsePaging.pageSize()).isEqualTo(pageSize);
            assertThat(responsePaging.hasNext()).isTrue();
            assertThat(responsePaging.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("cartItems not empty")
        void test3() {
            var itemSize = 5;
            var pageNumber = 1;
            var pageSize = 5;
            var rowSize = 3;
            var sortMethod = PRICE;
            var quantity = 3;
            String search = "text";

            var pageable = PageRequest.of(pageNumber - 1, pageSize, getSortMethod(sortMethod));
            var itemsPage = TestDataFactory.createItemPage(itemSize, 10, pageable);
            var firstItem = itemsPage.getContent().getFirst();
            when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    eq(search), eq(search), any(Pageable.class))
            ).thenReturn(itemsPage);
            when(cartItemRepository.findByItemIds(anyList())).thenReturn(List.of(new CartItem(firstItem, quantity)));

            var response = itemService.getItems(search, sortMethod, pageNumber, pageSize);

            assertThat(response).isNotNull();

            assertThat(response.items())
                    .isNotEmpty()
                    .hasSize(Math.ceilDiv(itemSize, rowSize));
            assertThat(response.items().getFirst().size()).isEqualTo(rowSize);

            assertThat(response.search()).isEqualTo(search);
            assertThat(response.sort()).isEqualTo(sortMethod);

            var responseFirstItem = response.items().getFirst().getFirst();
            assertThat(responseFirstItem.id()).isEqualTo(firstItem.getId());
            assertThat(responseFirstItem.title()).isEqualTo(firstItem.getTitle());
            assertThat(responseFirstItem.description()).isEqualTo(firstItem.getDescription());
            assertThat(responseFirstItem.imgPath()).isEqualTo(firstItem.getImgPath());
            assertThat(responseFirstItem.price()).isEqualTo(firstItem.getPrice());
            assertThat(responseFirstItem.count()).isEqualTo(quantity);

            var responsePaging = response.paging();
            assertThat(responsePaging.pageNumber()).isEqualTo(pageNumber);
            assertThat(responsePaging.pageSize()).isEqualTo(pageSize);
            assertThat(responsePaging.hasNext()).isTrue();
            assertThat(responsePaging.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("items empty")
        void test4() {
            var pageNumber = 1;
            var pageSize = 5;
            var sortMethod = ALPHA;
            var pageable = PageRequest.of(pageNumber - 1, pageSize, getSortMethod(sortMethod));
            var page = new PageImpl<Item>(Collections.emptyList(), pageable, 10);

            when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);

            var response = itemService.getItems(null, sortMethod, pageNumber, pageSize);

            assertThat(response).isNotNull();
            assertThat(response.items()).isEmpty();

            assertThat(response.search()).isNull();
            assertThat(response.sort()).isEqualTo(sortMethod);

            var responsePaging = response.paging();
            assertThat(responsePaging.pageNumber()).isEqualTo(pageNumber);
            assertThat(responsePaging.pageSize()).isEqualTo(pageSize);
            assertThat(responsePaging.hasNext()).isTrue();
            assertThat(responsePaging.hasPrevious()).isFalse();

            verify(cartItemRepository, never()).findByItemIds(anyList());
        }
    }


    @Nested
    @DisplayName("getItem")
    class getItem {

        @Test
        @DisplayName("ok")
        void test1() {
            var itemId = 1L;
            var item = TestDataFactory.createItem(itemId);
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
            when(cartItemRepository.findByItemId(anyLong())).thenReturn(Optional.empty());

            var response = itemService.getItem(itemId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(item.getId());
            assertThat(response.title()).isEqualTo(item.getTitle());
            assertThat(response.description()).isEqualTo(item.getDescription());
            assertThat(response.imgPath()).isEqualTo(item.getImgPath());
            assertThat(response.price()).isEqualTo(item.getPrice());
            assertThat(response.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("ok with quantity")
        void test2() {
            var itemId = 1L;
            var itemQuantity = 5;
            var item = TestDataFactory.createItem(itemId);
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
            when(cartItemRepository.findByItemId(anyLong()))
                    .thenReturn(Optional.of(new CartItem(item, itemQuantity)));

            var response = itemService.getItem(itemId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(item.getId());
            assertThat(response.title()).isEqualTo(item.getTitle());
            assertThat(response.description()).isEqualTo(item.getDescription());
            assertThat(response.imgPath()).isEqualTo(item.getImgPath());
            assertThat(response.price()).isEqualTo(item.getPrice());
            assertThat(response.count()).isEqualTo(itemQuantity);
        }

        @Test
        @DisplayName("throw")
        void test3() {
            var itemId = 1L;
            when(itemRepository.findById(itemId)).thenReturn(Optional.empty());
            assertThatExceptionOfType(ItemNotFoundException.class)
                    .isThrownBy(() -> itemService.getItem(itemId));
        }
    }


    @Nested
    @DisplayName("getCart")
    class getCart {

        @Test
        @DisplayName("ok")
        void test1() {
            var cartItem1 = TestDataFactory.createCartItem(1L, 1);
            var cartItem2 = TestDataFactory.createCartItem(2L, 2);

            var totalPrice = (cartItem1.getItem().getPrice() * cartItem1.getQuantity())
                    + (cartItem2.getItem().getPrice() * cartItem2.getQuantity());

            when(cartItemRepository.findAllFetch()).thenReturn(List.of(cartItem1, cartItem2));

            var response = itemService.getCart();

            assertThat(response).isNotNull();
            assertThat(response.items())
                    .isNotEmpty()
                    .hasSize(2);
            assertThat(response.total()).isEqualTo(totalPrice);

            var expectedItem = cartItem1.getItem();
            var responseItem = response.items().getFirst();
            assertThat(responseItem.id()).isEqualTo(expectedItem.getId());
            assertThat(responseItem.title()).isEqualTo(expectedItem.getTitle());
            assertThat(responseItem.description()).isEqualTo(expectedItem.getDescription());
            assertThat(responseItem.imgPath()).isEqualTo(expectedItem.getImgPath());
            assertThat(responseItem.price()).isEqualTo(expectedItem.getPrice());
            assertThat(responseItem.count()).isEqualTo(cartItem1.getQuantity());
        }

        @Test
        @DisplayName("empty")
        void test2() {
            when(cartItemRepository.findAllFetch()).thenReturn(Collections.emptyList());

            var response = itemService.getCart();

            assertThat(response).isNotNull();
            assertThat(response.items()).isEmpty();
            assertThat(response.total()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("updateItemsCountInCart")
    class updateItemsCountInCart {

        @Nested
        @DisplayName("increment")
        class increment {

            @Test
            @DisplayName("quantity = 0")
            void test1() {
                when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());
                when(itemRepository.findById(1L)).thenReturn(Optional.of(TestDataFactory.createItem(1L)));

                itemService.updateItemsCountInCart(1L, CartAction.PLUS);

                verify(itemRepository, times(1)).findById(1L);

                ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
                verify(cartItemRepository, times(1)).save(captor.capture());

                assertThat(captor.getValue().getQuantity()).isEqualTo(1);
                assertThat(captor.getValue().getItem().getId()).isEqualTo(1L);
            }

            @Test
            @DisplayName("quantity = 1")
            void test2() {
                var cartItem = Optional.of(TestDataFactory.createCartItem(1L, 1));
                when(cartItemRepository.findByItemId(1L)).thenReturn(cartItem);

                itemService.updateItemsCountInCart(1L, CartAction.PLUS);

                ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
                verify(cartItemRepository, times(1)).save(captor.capture());

                assertThat(captor.getValue().getQuantity()).isEqualTo(2);
                assertThat(captor.getValue().getItem().getId()).isEqualTo(1L);
            }
        }

        @Nested
        @DisplayName("decrement")
        class decrement {

            @Test
            @DisplayName("quantity = 0")
            void test1() {
                when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());
                assertThatExceptionOfType(CartItemNotFoundException.class)
                        .isThrownBy(() -> itemService.updateItemsCountInCart(1L, CartAction.MINUS))
                        .withMessage("Cart item with id = 1 not found.");
            }

            @Test
            @DisplayName("quantity = 1")
            void test2() {
                var cartItem = Optional.of(TestDataFactory.createCartItem(1L, 1));
                when(cartItemRepository.findByItemId(1L)).thenReturn(cartItem);

                itemService.updateItemsCountInCart(1L, CartAction.MINUS);

                verify(cartItemRepository, times(1)).delete(any(CartItem.class));
            }

            @Test
            @DisplayName("quantity = 2")
            void test3() {
                var cartItem = Optional.of(TestDataFactory.createCartItem(1L, 2));
                when(cartItemRepository.findByItemId(1L)).thenReturn(cartItem);

                itemService.updateItemsCountInCart(1L, CartAction.MINUS);

                ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
                verify(cartItemRepository, times(1)).save(captor.capture());

                assertThat(captor.getValue().getQuantity()).isEqualTo(1);
                assertThat(captor.getValue().getItem().getId()).isEqualTo(1L);
            }
        }

        @Nested
        @DisplayName("delete")
        class delete {

            @Test
            @DisplayName("ok")
            void test1() {
                var cartItem = Optional.of(TestDataFactory.createCartItem(1L, 10));
                when(cartItemRepository.findByItemId(1L)).thenReturn(cartItem);

                itemService.updateItemsCountInCart(1L, CartAction.DELETE);

                verify(cartItemRepository, times(1)).delete(any(CartItem.class));
            }

            @Test
            @DisplayName("throw")
            void test2() {
                when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());
                assertThatExceptionOfType(CartItemNotFoundException.class)
                        .isThrownBy(() -> itemService.updateItemsCountInCart(1L, CartAction.DELETE))
                        .withMessage("Cart item with id = 1 not found.");
            }
        }
    }

    private Sort getSortMethod(SortMethod sortMethod) {
        return switch (sortMethod) {
            case NO -> Sort.unsorted();
            case ALPHA -> Sort.by(ALPHA.getColumnName());
            case PRICE -> Sort.by(PRICE.getColumnName());
        };
    }
}
