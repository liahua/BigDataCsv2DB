package com.chenahua.transform.controller;

import com.chenahua.transform.protobuf.PersonProto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ProtoControllerTest {

    @Test
    void parseProto() throws IOException {
        PersonProto.Person person = PersonProto.Person.parseFrom(Files.newInputStream(Paths.get("/home/liahua/IdeaProjects/BigDataCsv2DB/.idea/httpRequests/2023-07-26T093957.200.protobuf")));
        System.out.println(person);

    }
}