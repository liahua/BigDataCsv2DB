package com.chenahua.transform.controller;

import com.chenahua.transform.protobuf.PersonProto;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @code <a href="https://spring.io/blog/2015/03/22/using-google-protocol-buffers-with-spring-mvc-based-rest-services">spring boot proto demo</a>
 */
@RestController
@RequestMapping("/test")
public class ProtoController {

    @RequestMapping("/proto")
    public PersonProto.Person protoPerson(String name, Integer age) {
        return PersonProto.Person.newBuilder().setName(name).setAge(age).build();
    }
}
