package org.springframework.samples.petclinic.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetPageDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.samples.petclinic.rest.dto.VisitDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.samples.petclinic.service.clinicService.ApplicationTestConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.web.WebAppConfiguration;

@SpringBootTest
@ContextConfiguration(classes = ApplicationTestConfig.class)
@WebAppConfiguration
class PetMapperTests {

    @MockitoBean
    private ClinicService clinicService;

    @Autowired
    private PetMapper petMapper;

    private static Pet pet(int id, String name) {
        Owner owner = new Owner();
        owner.setId(1);
        PetType type = new PetType();
        type.setId(2);
        type.setName("dog");
        Pet pet = new Pet();
        pet.setId(id);
        pet.setName(name);
        pet.setBirthDate(LocalDate.of(2018, 3, 4));
        pet.setType(type);
        pet.setOwner(owner);
        return pet;
    }

    private static void addVisit(Pet pet, int id, LocalDate date, String description) {
        Visit visit = new Visit();
        visit.setId(id);
        visit.setDate(date);
        visit.setDescription(description);
        pet.addVisit(visit);
    }

    private static PetDto petDto(String name, LocalDate birthDate, PetTypeDto type, Integer id,
                                 Integer ownerId, List<VisitDto> visits) {
        PetDto dto = new PetDto();
        dto.setName(name);
        dto.setBirthDate(birthDate);
        dto.setType(type);
        dto.setId(id);
        dto.setOwnerId(ownerId);
        dto.setVisits(visits);
        return dto;
    }

    private static VisitDto visitDto(LocalDate date, String description, Integer id, Integer petId) {
        VisitDto dto = new VisitDto();
        dto.setDate(date);
        dto.setDescription(description);
        dto.setId(id);
        dto.setPetId(petId);
        return dto;
    }

    @Test
    void mapsPetToDtoWithOwnerIdTypeAndSortedVisits() {
        Pet pet = pet(3, "Rosy");
        addVisit(pet, 10, LocalDate.of(2020, 1, 1), "older");
        addVisit(pet, 11, LocalDate.of(2021, 1, 1), "newer");

        PetDto dto = petMapper.toPetDto(pet);

        assertThat(dto.getId()).isEqualTo(3);
        assertThat(dto.getName()).isEqualTo("Rosy");
        assertThat(dto.getBirthDate()).isEqualTo(LocalDate.of(2018, 3, 4));
        assertThat(dto.getOwnerId()).isEqualTo(1);
        assertThat(dto.getType().getId()).isEqualTo(2);
        assertThat(dto.getType().getName()).isEqualTo("dog");
        assertThat(dto.getVisits()).extracting(VisitDto::getDescription).containsExactly("newer", "older");
        assertThat(dto.getVisits()).extracting(VisitDto::getPetId).containsOnly(3);
    }

    @Test
    void mapsDtoToPetWithOwnerId() {
        PetTypeDto type = new PetTypeDto("cat", 1);
        PetDto dto = petDto("Leo", LocalDate.of(2010, 9, 7), type, 5, 7,
            List.of(visitDto(LocalDate.of(2020, 1, 1), "checkup", 9, 5)));

        Pet pet = petMapper.toPet(dto);

        assertThat(pet.getId()).isEqualTo(5);
        assertThat(pet.getName()).isEqualTo("Leo");
        assertThat(pet.getOwner().getId()).isEqualTo(7);
        assertThat(pet.getType().getName()).isEqualTo("cat");
        assertThat(pet.getVisits()).hasSize(1);
        assertThat(pet.getVisits().get(0).getDescription()).isEqualTo("checkup");
    }

    @Test
    void mapsCollections() {
        Collection<PetDto> dtos = petMapper.toPetsDto(List.of(pet(1, "A"), pet(2, "B")));
        assertThat(dtos).extracting(PetDto::getName).containsExactly("A", "B");
        assertThat(petMapper.toPets(dtos)).extracting(Pet::getName).containsExactly("A", "B");
        assertThat(petMapper.toPetsDto(List.of())).isEmpty();
        assertThat(petMapper.toPets(List.of())).isEmpty();
    }

    @Test
    void nullPetMapsToNull() {
        assertThat(petMapper.toPetDto(null)).isNull();
        assertThat(petMapper.toPet((PetDto) null)).isNull();
    }

    @Test
    void mapsPageToPetPageDto() {
        PageRequest pageRequest = PageRequest.of(0, 2);
        PetPageDto page = petMapper.toPetPageDto(new PageImpl<>(List.of(pet(1, "A")), pageRequest, 1));
        assertThat(page.getContent()).extracting(PetDto::getName).containsExactly("A");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
