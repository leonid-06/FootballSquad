package com.leonid.models;

import lombok.Data;

@Data
public class Player {
    private String name;
    private String userName;
    private int rating = -1;
    private boolean isMatchCapitan;
    private boolean isBelongTeam;
    private boolean forConsideration;
    private boolean isActiveOnWeek;

    public void setName(String firstName, String lastName) {
        if (lastName==null && firstName==null) this.name = "no-name";
        else if (lastName==null) this.name=firstName;
        else if (firstName==null) this.name=lastName;
        else this.name = firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return name + " " + userName + " " + rating;
    }

    public void copyFields(Player player) {
        this.rating = player.getRating();
        this.isMatchCapitan = player.isMatchCapitan;
        this.forConsideration = player.forConsideration;
        this.isActiveOnWeek = player.isActiveOnWeek;
    }
}
