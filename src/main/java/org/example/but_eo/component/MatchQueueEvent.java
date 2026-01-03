package org.example.but_eo.component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class MatchQueueEvent {
    private final String sportType;
    private final String region;
}
