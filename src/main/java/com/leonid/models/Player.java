package com.leonid.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
public class Player {
    private String name;
    private String userName;
    private int rating;
    private boolean isMatchCapitan;
    private boolean isBelongTeam;
    private boolean forConsideration;

    @Override
    public String toString() {
        return name + " " + userName + " " + rating;
    }
}
