package ru.practicum.market.web.bind;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import ru.practicum.market.domain.exception.MarketBadRequestException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueryBinder")
class QueryBinderTest {

    private final QueryBinder binder = new QueryBinder();

    @Mock
    private ServerRequest request;

    @Nested
    @DisplayName("bindItemsQuery")
    class BindItemsQuery {

        @Test
        @DisplayName("invalid sort")
        void test1() {
            when(request.queryParam("search")).thenReturn(Optional.empty());
            when(request.queryParam("sort")).thenReturn(Optional.of("UNKNOWN"));

            assertThatExceptionOfType(MarketBadRequestException.class)
                    .isThrownBy(() -> binder.bindItemsQuery(request));


        }
    }
}
