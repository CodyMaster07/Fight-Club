package com.system;

import com.entities.Entity;
import com.entities.Fighter;
import com.entities.Team;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SaveTest {
    @TempDir
    Path directory;

    static save fixture() {
        save.FighterState boxer = new save.FighterState(Team.RED, false,
                100, 3, 2, 7, 4, "/res/fightersheet.png", 0);
        save.FighterState bomber = new save.FighterState(Team.BLUE, true,
                80, 3, 1, 5, 8, "/res/fightersheet.png", 4);
        return new save(1, 800, 600, 2,
                new save.RingState(150, 50, 150, 50, 500, 500, 2, 1),
                List.of(
                        entity("Boxer", 151, 51, 76, null, boxer, 13),
                        entity("Bomber", 390, 250, 68, null, bomber, 4),
                        entity("Ball", 180, 200, 1, "/res/ballRed.png", null, 0),
                        entity("Bomb", 300, 300, 1, "/res/ballBlue.png", null, 0)),
                0, 1, 123456);
    }

    private static save.EntityState entity(String type, double x, double y, int health,
            String sprite, save.FighterState fighter, int punch) {
        return new save.EntityState(type, x, y, Math.PI, fighter == null ? 32 : 48,
                fighter == null ? 32 : 68, 4, health, fighter == null ? 1 : 2,
                19, 0.2, 0.3, 654321, sprite, fighter, punch);
    }

    @Test
    void writesReadableJsonAndRestoresEveryAttribute() throws Exception {
        Path file = directory.resolve("saves/partida.json");
        save original = fixture();
        original.write(file);
        JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals("Boxer", json.getAsJsonArray("entities").get(0)
                .getAsJsonObject().get("type").getAsString());
        save loaded = save.read(file);
        assertEquals(original, loaded);

        Ring ring = Ring.fromState(loaded.ring());
        List<Entity> entities = loaded.restoreEntities(ring);
        assertEquals(loaded.ring(), ring.captureState());
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            assertEquals(loaded.entities().get(i), entity.captureState());
            assertSame(ring, field(entity, Entity.class, "ring"));
            BufferedImage image = (BufferedImage) field(entity, Entity.class, "sprite");
            assertNotNull(image);
            if (entity instanceof Fighter) {
                save.FighterState state = entity.captureState().fighter();
                int column = state.isLeft() ? 2 - state.animationFrame()
                        : 3 + state.animationFrame();
                int row = state.spriteFirstRow() + state.team().getRowOffset()
                        + (state.isLeft() ? 1 : 0);
                BufferedImage sheet = ImageIO.read(getClass().getResource(state.spriteSheetPath()));
                assertArrayEquals(sheet.getRGB(column * 34, row * 34, 34, 34, null, 0, 34),
                        image.getRGB(0, 0, 34, 34, null, 0, 34));
            }
        }
    }

    @Test
    void loadedSimulationContinuesWithTheSameTimersAndRandomChoices() throws Exception {
        save before = fixture();
        Path file = directory.resolve("partida.json");
        before.write(file);
        save after = save.read(file);
        Ring firstRing = Ring.fromState(before.ring());
        Ring secondRing = Ring.fromState(after.ring());
        List<Entity> first = before.restoreEntities(firstRing);
        List<Entity> second = after.restoreEntities(secondRing);

        for (int tick = 0; tick < 50; tick++) {
            advance(first, firstRing, tick);
            advance(second, secondRing, tick);
            assertEquals(firstRing.captureState(), secondRing.captureState());
            assertEquals(first.stream().map(Entity::captureState).toList(),
                    second.stream().map(Entity::captureState).toList());
        }
    }

    private static void advance(List<Entity> entities, Ring ring, int tick) {
        entities.forEach(Entity::tick);
        if (tick == 17 || tick == 40) {
            entities.get(0).onCollision(entities.get(1));
            entities.get(1).onCollision(entities.get(0));
        }
        entities.stream().filter(Fighter.class::isInstance).map(Fighter.class::cast)
                .forEach(Fighter::applyPendingDamage);
        ring.tick();
    }

    @Test
    void randomStateResumesTheNextDrawInsteadOfRestartingTheSeed() {
        GameRandom original = new GameRandom(123);
        for (int i = 0; i < 37; i++) {
            original.nextDouble();
        }
        GameRandom loaded = GameRandom.fromState(original.getState());
        for (int i = 0; i < 100; i++) {
            assertEquals(original.nextDouble(), loaded.nextDouble());
            assertEquals(original.nextInt(1, 5), loaded.nextInt(1, 5));
        }
    }

    @Test
    void missingCorruptAndIncompatibleFilesFailWithoutStartingANewMatch() throws Exception {
        Path file = directory.resolve("partida.json");
        assertThrows(NoSuchFileException.class, () -> save.read(file));
        for (String invalid : List.of("", "null", "{", "{}", "{\"version\":99}",
                Files.readString(writeFixture(file)) + " trailing data")) {
            Files.writeString(file, invalid);
            assertThrows(IOException.class, () -> save.read(file), invalid);
        }
        fixture().write(file);
        JsonObject invalid = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        invalid.getAsJsonArray("entities").get(0).getAsJsonObject()
                .addProperty("type", "Unknown");
        Files.writeString(file, invalid.toString());
        assertThrows(IOException.class, () -> save.read(file));
    }

    @Test
    void rejectsBrokenTeamReferencesAndInvalidAnimationFrames() {
        save state = fixture();
        assertThrows(IllegalArgumentException.class, () -> new save(1, 800, 600, 2,
                state.ring(), state.entities(), 1, 0, state.randomState()));
        assertThrows(IllegalArgumentException.class, () -> new save.FighterState(
                Team.RED, false, 100, 3, 3, 0, 0, "/res/fightersheet.png", 0));
    }

    @Test
    void overwritesThePreviousSaveWithoutLeavingTemporaryFiles() throws Exception {
        Path file = directory.resolve("partida.json");
        save state = fixture();
        state.write(file);
        save newer = new save(1, 1024, 768, 1, state.ring(), state.entities(),
                0, 1, 999);
        newer.write(file);
        assertEquals(newer, save.read(file));
        try (var files = Files.list(directory)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    private Path writeFixture(Path file) throws IOException {
        fixture().write(file);
        return file;
    }

    private static Object field(Object object, Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }
}
