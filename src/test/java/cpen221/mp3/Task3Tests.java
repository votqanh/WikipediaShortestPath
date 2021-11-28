package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Task3Tests {
    private WikiMediator mediator = new WikiMediator(100, 10);


    @Test
    public void searchDog() {
        ArrayList<String> list = new ArrayList<>();
        list.add("poodle");
        Assertions.assertEquals(list, mediator.search("dog",10));
    }
}
