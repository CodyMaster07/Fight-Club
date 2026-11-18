package com.system;

import com.entities.Ball;
import com.entities.Bomb;
import com.entities.Bomber;
import com.entities.Boxer;
import com.entities.Entity;
import com.entities.Fighter;
import com.entities.Team;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Estado da partida em JSON. O nome segue o solicitado para o trabalho. */
public record save(
        int version,
        int screenWidth,
        int screenHeight,
        int frameScale,
        RingState ring,
        List<EntityState> entities,
        int redFighterIndex,
        int blueFighterIndex,
        long randomState) {

    public static final Path DEFAULT_FILE = Path.of("saves", "partida.json");
    private static final int CURRENT_VERSION = 1;
    private static final Gson JSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .setStrictness(Strictness.STRICT)
            .disableJdkUnsafe()
            .create();

    public save {
        require(version == CURRENT_VERSION, "Versão de save incompatível");
        require(screenWidth > 0 && screenHeight > 0 && frameScale > 0
                && (long) screenWidth * frameScale <= Integer.MAX_VALUE
                && (long) screenHeight * frameScale <= Integer.MAX_VALUE,
                "Resolução ou escala inválida");
        Objects.requireNonNull(ring, "Ringue ausente");
        entities = List.copyOf(Objects.requireNonNull(entities, "Entidades ausentes"));
        validateActive(entities, redFighterIndex, Team.RED);
        validateActive(entities, blueFighterIndex, Team.BLUE);
        GameRandom.fromState(randomState);
    }

    public static save capture(int width, int height, int scale, Ring ring,
            List<Entity> entities, Fighter red, Fighter blue, GameRandom random) {
        return new save(CURRENT_VERSION, width, height, scale, ring.captureState(),
                entities.stream().map(Entity::captureState).toList(),
                activeIndex(entities, red), activeIndex(entities, blue), random.getState());
    }

    private static int activeIndex(List<Entity> entities, Fighter fighter) {
        if (fighter == null) {
            return -1;
        }
        int index = entities.indexOf(fighter);
        require(index >= 0, "Lutador ativo fora da lista de entidades");
        return index;
    }

    private static void validateActive(List<EntityState> entities, int index, Team team) {
        require(index >= -1 && index < entities.size(), "Índice de lutador inválido");
        int found = -1;
        for (int i = 0; i < entities.size(); i++) {
            FighterState fighter = entities.get(i).fighter();
            if (fighter != null && fighter.team() == team) {
                require(found == -1, "Mais de um lutador ativo no mesmo time");
                found = i;
            }
        }
        require(found == index, "Referência de lutador ativo inválida");
    }

    public List<Entity> restoreEntities(Ring restoredRing) {
        List<Entity> restored = new ArrayList<>();
        for (EntityState entity : entities) {
            restored.add(switch (entity.type()) {
                case "Boxer" -> new Boxer(entity, restoredRing);
                case "Bomber" -> new Bomber(entity, restoredRing);
                case "Ball" -> new Ball(entity, restoredRing);
                case "Bomb" -> new Bomb(entity, restoredRing);
                default -> throw new IllegalArgumentException("Tipo de entidade inválido");
            });
        }
        return restored;
    }

    public void write(Path file) throws IOException {
        Path target = file.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "partida-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                JSON.toJson(this, writer);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static save read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            save state = JSON.fromJson(reader, save.class);
            if (state == null) {
                throw new IOException("O arquivo de save está vazio");
            }
            return state;
        } catch (RuntimeException e) {
            throw new IOException("Arquivo de save inválido ou incompatível", e);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean finite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    public record RingState(double x, double y, int originalX, int originalY,
            int width, int height, int collidedSide, int counter) {
        public RingState {
            require(finite(x, y) && width > 0 && height > 0,
                    "Dimensões do ringue inválidas");
            require(collidedSide >= 0 && collidedSide <= 4 && counter >= 0 && counter < 10,
                    "Animação do ringue inválida");
        }
    }

    public record FighterState(Team team, boolean isLeft, int maxDurability,
            int normalFrameCount, int animationFrame, int damageFlashTicks,
            int pendingDamage, String spriteSheetPath, int spriteFirstRow) {
        public FighterState {
            Objects.requireNonNull(team, "Time ausente");
            require(maxDurability > 0 && normalFrameCount == 3
                    && animationFrame >= 0 && animationFrame < normalFrameCount
                    && damageFlashTicks >= 0 && damageFlashTicks <= 12
                    && pendingDamage >= 0, "Estado do lutador inválido");
            require(spriteSheetPath != null && spriteSheetPath.startsWith("/res/")
                    && spriteFirstRow >= 0, "Spritesheet inválida");
        }
    }

    public record EntityState(String type, double x, double y, double orientation,
            int maskWidth, int maskHeight, double speed, int durability,
            int spriteScale, int counter, double rand1, double rand2,
            long randomState, String spritePath, FighterState fighter,
            int punchTicksRemaining) {
        public EntityState {
            require(type != null && List.of("Boxer", "Bomber", "Ball", "Bomb").contains(type),
                    "Tipo de entidade desconhecido");
            require(finite(x, y, orientation, speed, rand1, rand2)
                    && maskWidth > 0 && maskHeight > 0 && speed >= 0
                    && durability >= 0 && spriteScale > 0 && counter >= 0 && counter < 20
                    && rand1 >= 0 && rand1 < 1 && rand2 >= 0 && rand2 < 1,
                    "Atributos da entidade inválidos");
            GameRandom.fromState(randomState);
            boolean isFighter = type.equals("Boxer") || type.equals("Bomber");
            require(isFighter == (fighter != null), "Tipo e estado do lutador não correspondem");
            if (isFighter) {
                require(durability > 0 && durability <= fighter.maxDurability()
                        && fighter.pendingDamage() <= durability
                        && punchTicksRemaining >= 0 && punchTicksRemaining <= 20
                        && fighter.spriteFirstRow() == (type.equals("Boxer") ? 0 : 4),
                        "Estado de combate inválido");
            } else {
                require(spritePath != null && spritePath.startsWith("/res/")
                        && punchTicksRemaining == 0, "Sprite da entidade inválida");
            }
        }
    }
}
