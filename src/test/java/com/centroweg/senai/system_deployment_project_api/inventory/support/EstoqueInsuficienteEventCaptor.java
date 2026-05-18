package com.centroweg.senai.system_deployment_project_api.inventory.support;

import com.centroweg.senai.system_deployment_project_api.inventory.EstoqueInsuficienteEvent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EstoqueInsuficienteEventCaptor {

    private final List<EstoqueInsuficienteEvent> events = new ArrayList<>();

    @EventListener
    public void capture(EstoqueInsuficienteEvent event) {
        events.add(event);
    }

    public List<EstoqueInsuficienteEvent> getEvents() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
