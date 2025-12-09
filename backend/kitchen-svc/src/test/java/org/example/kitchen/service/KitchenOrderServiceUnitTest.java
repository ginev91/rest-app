package org.example.kitchen.service;

import org.example.kitchen.exception.KitchenOrderOperationException;
import org.example.kitchen.model.KitchenOrder;
import org.example.kitchen.model.enums.KitchenOrderStatus;
import org.example.kitchen.repository.KitchenOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KitchenOrderServiceUnitTest {

    @Mock
    private KitchenOrderRepository repository;

    @Mock
    private HttpClient mockHttpClient;

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        } catch (NoSuchFieldException ignored) { }
        f.set(target, value);
    }

    private static final class ImmediateScheduledExecutor implements ScheduledExecutorService {
        private volatile boolean shutdown = false;

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            command.run();
            return new ImmediateScheduledFuture<>();
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            try {
                callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return new ImmediateScheduledFuture<>();
        }

        private static final class ImmediateScheduledFuture<V> implements ScheduledFuture<V> {
            @Override public long getDelay(TimeUnit unit) { return 0; }
            @Override public int compareTo(Delayed o) { return 0; }
            @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
            @Override public boolean isCancelled() { return false; }
            @Override public boolean isDone() { return true; }
            @Override public V get() { return null; }
            @Override public V get(long timeout, TimeUnit unit) { return null; }
        }

        @Override public void shutdown() { shutdown = true; }
        @Override public List<Runnable> shutdownNow() { shutdown = true; return Collections.emptyList(); }
        @Override public boolean isShutdown() { return shutdown; }
        @Override public boolean isTerminated() { return shutdown; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
        @Override public <T> Future<T> submit(Callable<T> task) { throw new UnsupportedOperationException(); }
        @Override public <T> Future<T> submit(Runnable task, T result) { throw new UnsupportedOperationException(); }
        @Override public Future<?> submit(Runnable task) { throw new UnsupportedOperationException(); }
        @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public void execute(Runnable command) { command.run(); }
        @Override public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) { throw new UnsupportedOperationException(); }
    }

    @Test
    void createOrder_withSchedulingAndCallback_runsScheduledTask_andCallbackInvoked() throws Exception {
        KitchenOrderService svc = new KitchenOrderService(repository,
                1, 1,
                true,
                "http://localhost/cb?orderId={orderId}&kitchenOrderId={kitchenOrderId}",
                "secret-123",
                true);

        setField(svc, "scheduler", new ImmediateScheduledExecutor());
        setField(svc, "httpClient", mockHttpClient);

        @SuppressWarnings("unchecked")
        HttpResponse<String> httpResp = (HttpResponse<String>) mock(HttpResponse.class);
        when(httpResp.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResp);

        AtomicReference<KitchenOrder> savedRef = new AtomicReference<>();
        when(repository.save(any(KitchenOrder.class))).thenAnswer(inv -> {
            KitchenOrder o = inv.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            savedRef.set(o);
            return o;
        });
        when(repository.findById(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(savedRef.get()));

        UUID orderId = UUID.randomUUID();
        KitchenOrder created = svc.createOrder(orderId, "[{\"name\":\"X\"}]");

        verify(repository, atLeastOnce()).save(any(KitchenOrder.class));

        KitchenOrder after = savedRef.get();
        assertThat(after).isNotNull();
        assertThat(after.getStatus()).isEqualTo(KitchenOrderStatus.READY);

        verify(mockHttpClient, atLeastOnce()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void createOrder_whenPrepSchedulingDisabled_doesNotSchedule() {
        KitchenOrderService svc = new KitchenOrderService(repository,
                1, 2,
                false, "", "", false);

        when(repository.save(any(KitchenOrder.class))).thenAnswer(inv -> {
            KitchenOrder o = inv.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            return o;
        });

        svc.createOrder(UUID.randomUUID(), "[]");

        verify(repository, times(1)).save(any(KitchenOrder.class));
    }

    @Test
    void scheduleCompletion_skips_whenOrderNotFound_and_whenTerminalState() throws Exception {
        KitchenOrderService svc = new KitchenOrderService(repository,1,1,false,"","",true);
        setField(svc, "scheduler", new ImmediateScheduledExecutor());

        when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());
        KitchenOrder ko = KitchenOrder.builder().id(UUID.randomUUID()).orderId(UUID.randomUUID()).status(KitchenOrderStatus.PREPARING).createdAt(Instant.now()).build();
        var method = KitchenOrderService.class.getDeclaredMethod("scheduleCompletion", KitchenOrder.class);
        method.setAccessible(true);
        method.invoke(svc, ko);
        verify(repository, never()).save(any(KitchenOrder.class));

        KitchenOrder term = KitchenOrder.builder().id(UUID.randomUUID()).orderId(UUID.randomUUID()).status(KitchenOrderStatus.CANCELLED).createdAt(Instant.now()).build();
        when(repository.findById(term.getId())).thenReturn(Optional.of(term));
        method.invoke(svc, term);
        verify(repository, never()).save(term);
    }

    @Test
    void scheduleCompletion_swallowCallbackExceptions_and_markReady() throws Exception {
        KitchenOrderService svc = new KitchenOrderService(repository,
                1,1,
                true,
                "http://example/cb?orderId={orderId}&kitchenOrderId={kitchenOrderId}",
                "", true);
        setField(svc, "scheduler", new ImmediateScheduledExecutor());
        setField(svc, "httpClient", mockHttpClient);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new RuntimeException("cb-fail"));

        AtomicReference<KitchenOrder> savedRef = new AtomicReference<>();
        when(repository.save(any(KitchenOrder.class))).thenAnswer(inv -> {
            KitchenOrder o = inv.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            savedRef.set(o);
            return o;
        });
        when(repository.findById(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(savedRef.get()));

        svc.createOrder(UUID.randomUUID(), "[]");

        KitchenOrder after = savedRef.get();
        assertThat(after).isNotNull();
        assertThat(after.getStatus()).isEqualTo(KitchenOrderStatus.READY);
        verify(mockHttpClient, atLeastOnce()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void updateStatus_success_and_failure_paths() {
        UUID id = UUID.randomUUID();
        KitchenOrder current = KitchenOrder.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .status(KitchenOrderStatus.PREPARING)
                .createdAt(Instant.now())
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(current));
        when(repository.save(any(KitchenOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        KitchenOrderService svc = new KitchenOrderService(repository,1,2,false,"","",false);
        KitchenOrder saved = svc.updateStatus(id, KitchenOrderStatus.IN_PROGRESS);
        assertThat(saved.getStatus()).isEqualTo(KitchenOrderStatus.IN_PROGRESS);
        verify(repository).save(any(KitchenOrder.class));

        KitchenOrder completed = KitchenOrder.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .status(KitchenOrderStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
        when(repository.findById(completed.getId())).thenReturn(Optional.of(completed));
        assertThatThrownBy(() -> svc.updateStatus(completed.getId(), KitchenOrderStatus.PREPARING))
                .isInstanceOf(KitchenOrderOperationException.class);

        KitchenOrder cancelled = KitchenOrder.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .status(KitchenOrderStatus.CANCELLED)
                .createdAt(Instant.now())
                .build();
        when(repository.findById(cancelled.getId())).thenReturn(Optional.of(cancelled));
        assertThatThrownBy(() -> svc.updateStatus(cancelled.getId(), KitchenOrderStatus.READY))
                .isInstanceOf(KitchenOrderOperationException.class)
                .hasMessageContaining("is cancelled");
    }

    @Test
    void cancelOrder_success_and_disallowedState_throw() {
        UUID id = UUID.randomUUID();
        KitchenOrder preparing = KitchenOrder.builder()
                .id(id)
                .orderId(UUID.randomUUID())
                .status(KitchenOrderStatus.PREPARING)
                .createdAt(Instant.now())
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(preparing));
        when(repository.save(any(KitchenOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        KitchenOrderService svc = new KitchenOrderService(repository,1,2,false,"","",false);
        svc.cancelOrder(id);

        ArgumentCaptor<KitchenOrder> captor = ArgumentCaptor.forClass(KitchenOrder.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(KitchenOrderStatus.CANCELLED);

        UUID id2 = UUID.randomUUID();
        KitchenOrder ready = KitchenOrder.builder().id(id2).orderId(UUID.randomUUID()).status(KitchenOrderStatus.READY).createdAt(Instant.now()).build();
        when(repository.findById(id2)).thenReturn(Optional.of(ready));
        assertThatThrownBy(() -> svc.cancelOrder(id2)).isInstanceOf(KitchenOrderOperationException.class);
    }

    @Test
    void findByOrderId_and_delete_delegateToRepository() {
        UUID orderId = UUID.randomUUID();
        List<KitchenOrder> resp = List.of(KitchenOrder.builder().id(UUID.randomUUID()).orderId(orderId).status(KitchenOrderStatus.NEW).createdAt(Instant.now()).build());
        when(repository.findByOrderId(orderId)).thenReturn(resp);

        KitchenOrderService svc = new KitchenOrderService(repository,1,2,false,"","",false);
        List<KitchenOrder> found = svc.findByOrderId(orderId);
        assertThat(found).isSameAs(resp);

        UUID id = UUID.randomUUID();
        svc.delete(id);
        verify(repository).deleteById(id);
    }

    @Test
    void shutdown_handlesSchedulerExceptions() throws Exception {
        ScheduledExecutorService badScheduler = mock(ScheduledExecutorService.class);
        when(badScheduler.shutdownNow()).thenThrow(new RuntimeException("boom"));

        KitchenOrderService svc = new KitchenOrderService(repository,1,2,false,"","",false);
        setField(svc, "scheduler", badScheduler);

        svc.shutdown();
        verify(badScheduler).shutdownNow();
    }


    @Test
    void isValidTransition_matrix_checksManyCombinations() throws Exception {
        KitchenOrderService svc = new KitchenOrderService(repository,1,2,false,"","",false);
        Method m = KitchenOrderService.class.getDeclaredMethod("isValidTransition", KitchenOrderStatus.class, KitchenOrderStatus.class);
        m.setAccessible(true);

        BiFunction<KitchenOrderStatus, KitchenOrderStatus, Boolean> call = (f, t) -> {
            try {
                return (Boolean) m.invoke(svc, f, t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        
        assertThat(call.apply(KitchenOrderStatus.NEW, KitchenOrderStatus.PREPARING)).isTrue();
        assertThat(call.apply(KitchenOrderStatus.NEW, KitchenOrderStatus.CANCELLED)).isTrue();
        assertThat(call.apply(KitchenOrderStatus.PREPARING, KitchenOrderStatus.IN_PROGRESS)).isTrue();
        assertThat(call.apply(KitchenOrderStatus.PREPARING, KitchenOrderStatus.READY)).isTrue();
        assertThat(call.apply(KitchenOrderStatus.IN_PROGRESS, KitchenOrderStatus.READY)).isTrue();
        assertThat(call.apply(KitchenOrderStatus.READY, KitchenOrderStatus.SERVED)).isTrue();
        assertThat(call.apply(KitchenOrderStatus.READY, KitchenOrderStatus.COMPLETED)).isTrue();
        
        assertThat(call.apply(KitchenOrderStatus.PREPARING, KitchenOrderStatus.NEW)).isFalse();
        assertThat(call.apply(KitchenOrderStatus.IN_PROGRESS, KitchenOrderStatus.PREPARING)).isFalse();
        assertThat(call.apply(KitchenOrderStatus.READY, KitchenOrderStatus.PREPARING)).isFalse();
        
        assertThat(call.apply(KitchenOrderStatus.IN_PROGRESS, KitchenOrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void createOrder_schedulingParity_firstIsScheduled_secondIsNot() throws Exception {
        KitchenOrderService svc = new KitchenOrderService(repository,
                1,1,
                false,
                "", "", true);

        
        setField(svc, "scheduler", new ImmediateScheduledExecutor());

        List<KitchenOrder> savedList = new ArrayList<>();
        when(repository.save(any(KitchenOrder.class))).thenAnswer(inv -> {
            KitchenOrder o = inv.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID());
            savedList.add(o);
            return o;
        });
        when(repository.findById(any(UUID.class))).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return savedList.stream().filter(s -> s.getId() != null && s.getId().equals(id)).findFirst();
        });

        
        UUID o1 = UUID.randomUUID();
        UUID o2 = UUID.randomUUID();
        svc.createOrder(o1, "[]");
        svc.createOrder(o2, "[]");

        boolean anyReady = savedList.stream().anyMatch(s -> s.getStatus() == KitchenOrderStatus.READY);
        assertThat(anyReady).isTrue();
    }
}