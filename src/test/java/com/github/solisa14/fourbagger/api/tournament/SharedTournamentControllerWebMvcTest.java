package com.github.solisa14.fourbagger.api.tournament;

import com.github.solisa14.fourbagger.api.game.GameType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SharedTournamentController.class)
@AutoConfigureMockMvc(addFilters = false)
class SharedTournamentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TournamentService tournamentService;

    @MockitoBean
    private TournamentMapper tournamentMapper;

    @MockitoBean
    private com.github.solisa14.fourbagger.api.security.JwtService jwtService;

    @Test
    void getSharedTournament_whenAvailable_returnsNoStoreResponse() throws Exception {
        UUID shareId = UUID.randomUUID();
        Tournament tournament = Tournament.builder().build();
        SharedTournamentResponse response = new SharedTournamentResponse(
                "Family Tournament",
                TournamentStatus.IN_PROGRESS,
                GameType.SINGLES,
                TournamentFormat.SINGLE_ELIMINATION,
                new SharedTournamentResponse.Brackets(List.of(), List.of(), List.of(), List.of()));
        when(tournamentService.getSharedTournament(shareId)).thenReturn(tournament);
        when(tournamentMapper.toSharedTournamentResponse(tournament)).thenReturn(response);

        mockMvc.perform(get("/api/v1/shared-tournaments/{shareId}", shareId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.title").value("Family Tournament"))
                .andExpect(jsonPath("$.format").value("SINGLE_ELIMINATION"));
    }

    @Test
    void getSharedTournament_whenInvalidOrUnavailable_returnsSameNoStoreNotFound() throws Exception {
        UUID unknownShareId = UUID.randomUUID();
        when(tournamentService.getSharedTournament(unknownShareId)).thenThrow(new TournamentNotFoundException());

        for (String shareId : List.of("not-a-uuid", unknownShareId.toString())) {
            mockMvc.perform(get("/api/v1/shared-tournaments/{shareId}", shareId))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Tournament not found"));
        }
    }
}
