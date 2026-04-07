package io.arconia.cli.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JsonParser}.
 */
class JsonParserTests {

    record Person(String firstName, String lastName) {}

    @Test
    void toJsonSerializesRecord() {
        String json = JsonParser.toJson(new Person("John", "Doe"));
        assertThat(json).isEqualTo("""
                {"firstName":"John","lastName":"Doe"}""");
    }

    @Test
    void toJsonSerializesNullFieldAsNull() {
        String json = JsonParser.toJson(new Person("John", null));
        assertThat(json).isEqualTo("""
                {"firstName":"John","lastName":null}""");
    }

    @Test
    void toBytesMatchesToJsonBytes() {
        Person person = new Person("John", "Doe");
        assertThat(JsonParser.toBytes(person)).isEqualTo(JsonParser.toJson(person).getBytes());
    }

    @Test
    void fromJsonDeserializesRecord() {
        Person person = JsonParser.fromJson("""
                {
                    "firstName": "John",
                    "lastName": "Doe"
                }""", Person.class);
        assertThat(person.firstName()).isEqualTo("John");
        assertThat(person.lastName()).isEqualTo("Doe");
    }

    @Test
    void fromJsonIgnoresUnknownFields() {
        Person person = JsonParser.fromJson("""
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "unknown": "value"
                }""", Person.class);
        assertThat(person.firstName()).isEqualTo("John");
    }

    @Test
    void toJsonPrettyPrintIsIndented() {
        String json = JsonParser.toJsonPrettyPrint(new Person("John", "Doe"));
        assertThat(json).isEqualTo("""
                {
                  "firstName" : "John",
                  "lastName" : "Doe"
                }""");
    }

    @Test
    void roundTripPreservesData() {
        Person original = new Person("John", "Doe");
        Person roundTripped = JsonParser.fromJson(JsonParser.toJson(original), Person.class);
        assertThat(roundTripped).isEqualTo(original);
    }

}
