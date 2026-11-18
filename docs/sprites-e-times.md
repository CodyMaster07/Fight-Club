# Sprites por classe, time e direção

Este guia mostra as alterações para você aplicar no projeto. Os arquivos Java do jogo não foram alterados.

## 1. Entenda a grade

Use `/res/fighterspritefinal.png`, que contém os dois personagens. Os índices abaixo começam em zero:

| Classe | Time | Direção | Linha | Y esperado |
|---|---|---|---:|---:|
| Boxer | Vermelho | Direita | 0 | 0 |
| Boxer | Vermelho | Esquerda | 1 | 34 |
| Boxer | Azul | Direita | 2 | 68 |
| Boxer | Azul | Esquerda | 3 | 102 |
| Bomber | Vermelho | Direita | 4 | 136 |
| Bomber | Vermelho | Esquerda | 5 | 170 |
| Bomber | Azul | Direita | 6 | 204 |
| Bomber | Azul | Esquerda | 7 | 238 |

Cada classe carrega apenas suas quatro linhas. Dentro desse bloco, a linha é `team.getRowOffset() + (isLeft ? 1 : 0)`.

Como você espelhou **a linha inteira**, também inverteu a ordem das colunas:

| Direção | Normal: quadro lógico 0, 1, 2 | Dano: quadro lógico 0, 1, 2 |
|---|---|---|
| Direita | 0, 1, 2 | 3, 4, 5 |
| Esquerda | 5, 4, 3 | 2, 1, 0 |

Assim, a sequência lógica `{0, 1, 2, 1, 0}` produz `{2, 1, 0, 1, 2}` durante o dano à esquerda. A animação normal à esquerda produz `{5, 4, 3, 4, 5}`: o mesmo movimento, com deslocamento de três colunas em relação aos quadros de dano.

**Confira a exportação antes dos recortes.** A imagem atual mede **206 × 274**, enquanto essa grade mede **204 × 272**. Há conteúdo nos pixels excedentes, portanto não basta assumir que são margens transparentes. Comparando as linhas, as duas primeiras linhas esquerdas são espelhos da largura inteira de 206 px; isso desloca os recortes horizontais em 2 px quando você usa uma grade de 34 px. O espelho da linha azul do Bomber também aparece em Y = 239, em vez de 238. O código abaixo segue a grade regular que você descreveu: no editor, alinhe cada quadro em uma célula de 34 × 34 e reexporte em 204 × 272. Espelhe cada célula dentro da grade, preservando a disposição de colunas indicada na tabela, ou espelhe a linha inteira já com largura exata de 204 px.

## 2. Novo arquivo: `src/main/java/com/entities/Team.java`

O time escolhe a cor; a classe do lutador escolhe o personagem.

```java
package com.entities;

public enum Team {
    RED(0),
    BLUE(2);

    private final int rowOffset;

    Team(int rowOffset) {
        this.rowOffset = rowOffset;
    }

    public int getRowOffset() {
        return rowOffset;
    }
}
```

## 3. `src/main/java/com/entities/Fighter.java`

O `Fighter` deve guardar o time, a direção e os recortes. Mantenha o código de vida, dano pendente, barra de vida, `tick()` e `setAnimationFrame()` que você já tem.

Remova `totalFrames`, substitua a declaração antiga de `sprites` e acrescente os campos abaixo. Mantenha `F_WIDTH`, `F_HEIGHT`, `normalFrameCount`, `animationFrame`, `damageFlashTicks` e os demais campos existentes.

```java
private BufferedImage[][] sprites;
private final Team team;
private boolean isLeft;
```

Substitua o construtor por este. `isLeft` significa **olhando para a esquerda**, independentemente do lado em que o personagem nasceu ou da direção em que está andando.

```java
public Fighter(double x, double y, double ort,
               int maskWidth, int maskHeight,
               double speed, int durability, Team team) {
    super(x, y, ort, maskWidth, maskHeight, speed, durability, 2);
    this.maxDurability = durability;
    this.team = java.util.Objects.requireNonNull(team, "Time obrigatório");
}
```

Substitua **todo** o método `loadSpriteSheet()` pelo seguinte. `firstRow` é 0 para Boxer e 4 para Bomber; `frameCount` é 3, pois existem três poses por estado. Cada linha tem seis colunas: três normais e três de dano.

```java
protected void loadSpriteSheet(String spritePath, int firstRow, int frameCount) {
    if (firstRow < 0 || frameCount <= 0) {
        throw new IllegalArgumentException("Linha ou quantidade de quadros inválida");
    }

    URL url = getClass().getResource(spritePath);
    if (url == null) {
        throw new IllegalStateException("Sprite não encontrada: " + spritePath);
    }

    try {
        BufferedImage sheet = ImageIO.read(url);
        int columns = frameCount * 2;

        if (sheet == null
                || sheet.getWidth() < columns * F_WIDTH
                || sheet.getHeight() < (firstRow + 4) * F_HEIGHT) {
            throw new IllegalStateException("Spritesheet inválida: " + spritePath);
        }

        sprites = new BufferedImage[4][columns];

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < columns; column++) {
                sprites[row][column] = sheet.getSubimage(
                        column * F_WIDTH,
                        (firstRow + row) * F_HEIGHT,
                        F_WIDTH,
                        F_HEIGHT);
            }
        }

        normalFrameCount = frameCount;
        setAnimationFrame(0);
    } catch (IOException e) {
        throw new IllegalStateException("Não foi possível carregar: " + spritePath, e);
    }
}
```

No seu laço antigo, `for (int j = 0; j < 4; i++)` incrementava `i`, deixando `j` sempre em zero. Aqui cada laço incrementa seu próprio índice. A matriz também passa a guardar linha e coluna separadamente.

Substitua **todo** o método `updateSprite()`:

```java
private void updateSprite() {
    if (sprites == null) {
        return;
    }

    int row = team.getRowOffset() + (isLeft ? 1 : 0);
    boolean damaged = damageFlashTicks > 0;
    int column;

    if (isLeft) {
        int lastColumn = damaged
                ? normalFrameCount - 1       // 2: dano começa pelo último quadro
                : normalFrameCount * 2 - 1; // 5: normal começa pelo último quadro
        column = lastColumn - animationFrame;
    } else {
        int offset = damaged ? normalFrameCount : 0;
        column = offset + animationFrame;
    }

    sprite = sprites[row][column];
}
```

Acrescente estes métodos ao `Fighter`:

```java
public Team getTeam() {
    return team;
}

public boolean isLeft() {
    return isLeft;
}

public void faceOpponent(Fighter opponent) {
    if (opponent == null || opponent == this || opponent.getTeam() == team) {
        return;
    }

    double myCenterX = getX() + getRenderWidth() / 2.0;
    double opponentCenterX = opponent.getX() + opponent.getRenderWidth() / 2.0;

    // Na mesma coordenada horizontal, preserva a direção anterior.
    if (myCenterX == opponentCenterX) {
        return;
    }

    boolean nextIsLeft = opponentCenterX < myCenterX;
    if (isLeft != nextIsLeft) {
        isLeft = nextIsLeft;
        updateSprite();
    }
}
```

Quando o adversário está à esquerda, `isLeft = true`; à direita, `false`. Não calcule esse valor com `orientation`: no seu jogo, o personagem pode estar andando para longe do oponente e continuar olhando para ele.

## 4. `src/main/java/com/entities/Boxer.java`

Substitua o construtor:

```java
public Boxer(double x, double y, double ort, Team team) {
    super(x, y, ort, MASK_WIDTH, MASK_HEIGHT,
            DEFAULT_SPEED, DEFAULT_DURABILITY, team);

    loadSpriteSheet("/res/fighterspritefinal.png", 0, 3);
}
```

Mantenha `PUNCH_ANIMATION_FRAMES = {0, 1, 2, 1, 0}` e os métodos de animação. O `Boxer` informa a pose lógica; o `Fighter` encontra a coluna correspondente à direção e ao dano. Você não precisa duplicar a sequência de soco.

Para só iniciar o ataque ao tocar em um adversário, substitua `onCollision()`:

```java
@Override
public void onCollision(Entity other) {
    if (isKO()
            || !(other instanceof Fighter target)
            || target == this
            || target.isKO()
            || target.getTeam() == getTeam()) {
        return;
    }

    if (tryStartPunch()) {
        target.takeDamage(PUNCH_DAMAGE);
    }
}
```

Isso também impede que uma colisão com uma bola ou bomba inicie um soco sem alvo válido.

## 5. `src/main/java/com/entities/Bomber.java`

Substitua o construtor:

```java
public Bomber(double x, double y, double ort, Team team) {
    super(x, y, ort, MASK_WIDTH, MASK_HEIGHT,
            DEFAULT_SPEED, DEFAULT_DURABILITY, team);

    loadSpriteSheet("/res/fighterspritefinal.png", 4, 3);
}
```

Use o mesmo `onCollision()` mostrado para Boxer, enquanto Bomber continuar com o ataque de contato que existe hoje no seu código. A diferença visual é `firstRow = 4`. O lançamento de bombas pode ser implementado separadamente depois; mudar a spritesheet não cria esse comportamento.

As importações `IOException`, `URL`, `ImageIO` e `BufferedImage` podem ser removidas de Boxer e Bomber: quem recorta a imagem agora é apenas Fighter.

## 6. `src/main/java/com/system/Engine.java`

O controle de quem está em campo pertence à `Engine`. Cada time terá uma referência para seu lutador ativo. A lista `entities` deve conter somente os lutadores que estão em campo, além de eventuais projéteis ou outras entidades.

Acrescente as importações:

```java
import com.entities.Bomber;
import com.entities.Team;
```

Acrescente os campos:

```java
private Fighter redFighter;
private Fighter blueFighter;
```

Acrescente estes métodos:

```java
private void setActiveFighter(Fighter next) {
    if (next == null || next.isKO()) {
        throw new IllegalArgumentException("O lutador deve estar vivo");
    }

    if (next.getTeam() == Team.RED) {
        entities.remove(redFighter);
        redFighter = next;
    } else {
        entities.remove(blueFighter);
        blueFighter = next;
    }

    entities.add(next);
    updateFacing();
}

private void updateFacing() {
    if (redFighter == null || blueFighter == null) {
        return;
    }

    redFighter.faceOpponent(blueFighter);
    blueFighter.faceOpponent(redFighter);
}
```

Sempre use `setActiveFighter()` para colocar ou substituir um lutador. Ele retira o anterior daquele time antes de incluir o próximo. Não adicione os reservas diretamente em `entities`, pois eles também seriam atualizados, desenhados e envolvidos nas colisões.

Para começar com um Boxer vermelho e um Bomber azul, substitua **todo** o construtor antigo por este exemplo mínimo de partida. Você pode trocar as duas classes livremente sem mudar a lógica dos times.

```java
public Engine(int[] res, int[] ring, int frameScale) {
    entities = new ArrayList<>();
    window = new Window("Fight Club Game", res, frameScale);

    int ringWidth = ring[0];
    int ringHeight = ring[1];
    double ringX = (window.getNatWidth() - ringWidth) / 2.0;
    double ringY = (window.getNatHeight() - ringHeight) / 2.0;

    this.ring = Ring.init(ringX, ringY, ringWidth, ringHeight);

    // Ring.init precisa acontecer antes de construir qualquer Entity.
    // As posições abaixo deixam os personagens dentro do ringue.
    double fighterWidth = 34 * 2;
    double fighterHeight = 34 * 2;
    double y = ringY + (ringHeight - fighterHeight) / 2.0;
    double leftX = ringX + (ringWidth - fighterWidth) * 0.25;
    double rightX = ringX + (ringWidth - fighterWidth) * 0.75;

    setActiveFighter(new Boxer(leftX, y, Math.PI / 4.0, Team.RED));
    setActiveFighter(new Bomber(rightX, y, -3.0 * Math.PI / 4.0, Team.BLUE));
}
```

Esse construtor substitui os laços que criavam várias bolas e boxers. A antiga configuração por quantidades sai desse exemplo; o ajuste correspondente em Main está no próximo passo. Você também pode remover os imports de `Ball` e `Random` e o campo `random`, caso não os utilize em outro lugar.

Substitua o método `tick()` da Engine:

```java
public void tick() {
    for (Entity entity : entities) {
        entity.tick();
    }

    checkEntitiesCollision();

    for (Entity entity : entities) {
        if (entity instanceof Fighter fighter) {
            fighter.applyPendingDamage();
        }
    }

    entities.removeIf(entity -> entity instanceof Fighter fighter && fighter.isKO());

    if (redFighter != null && redFighter.isKO()) {
        redFighter = null;
    }
    if (blueFighter != null && blueFighter.isKO()) {
        blueFighter = null;
    }

    updateFacing();
    ring.tick();
}
```

A direção é atualizada depois de **todos** se moverem e de as colisões corrigirem suas posições. Assim os dois usam as posições finais do mesmo tick. O dano continua sendo aplicado em uma segunda passagem, preservando a possibilidade de nocaute simultâneo.

Para ter reservas futuramente, guarde uma fila separada para cada time. Depois de retirar os nocauteados, retire o próximo reserva da fila e passe para `setActiveFighter()`, antes de `updateFacing()`. Este exemplo já garante um ativo por time e permite substituições; ele não implementa escalação de reservas nem encerramento da partida.

## 7. `src/main/java/com/game/Main.java`

Como o construtor do exemplo recebe apenas resolução, ringue e escala, altere a chamada para:

```java
Engine engine = new Engine(res, ring, frameScale);
```

Remova os campos `players` e `initialPos`, a variável local `definedNPlayers`, o bloco completo dos casos `"-p"` / `"--players"` em `parseArgs()` e a linha de ajuda sobre essa opção. A seleção dos dois lutadores está explícita no construtor da Engine neste passo; a interface de seleção de times pode passar a fornecer essa escolha depois.

## 8. O que conferir ao executar

1. Boxer vermelho à esquerda e Bomber azul à direita devem começar olhando um para o outro.
2. Quando trocarem de lado, os dois devem trocar de linha sem mudar de cor ou classe.
3. Ao receber dano olhando para a direita, devem aparecer as colunas 3, 4 e 5.
4. Ao receber dano olhando para a esquerda, devem aparecer as colunas 2, 1 e 0, correspondentes às mesmas poses lógicas.
5. Ao substituir um lutador com `setActiveFighter()`, o anterior daquele time deve sair de `entities`.

O flash de dano mantém a duração atual de 12 ticks e acompanha o quadro lógico do ataque. Ele não vira uma animação independente de cinco passos; para isso seria necessário um contador específico de animação de dano.
