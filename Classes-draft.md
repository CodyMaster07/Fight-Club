# Estrutura de Classes do Jogo - Rascunho POO

## 1. Entidades Base (Abstração e Herança)

**Classe Abstrata: `EntidadeJogo`**
*   **Propriedades:** `x`, `y`, `velocidadeX`, `velocidadeY`, `raio` (para colisão circular)
*   **Métodos:** `mover()`, `renderizar()`, `verificarColisaoParede(limites)`

**Classe Abstrata: `Lutador` (Herda de `EntidadeJogo`)**
*   **Propriedades:** `vidaMax`, `vidaAtual`, `nome`
*   **Métodos:** 
    *   `receberDano(quantidade)` (Encapsulamento da vida)
    *   `colidirCom(Lutador outro)` (Lógica de ricochete com variação de 30 graus)
    *   `atacar()` (Método abstrato - Polimorfismo)

## 2. Tipos de Lutadores (Herança e Polimorfismo)

**Classe: `LancadorAdagas` (Herda de `Lutador`)**
*   **Métodos:**
    *   `colidirCom(Lutador outro)`: Sobrescreve para aplicar ricochete, mas *sem* causar dano de contato.
    *   `atacar()`: Instancia um objeto `ProjetilAdaga` na direção do oponente.

**Classe: `Magico` (Herda de `Lutador`)**
*   **Propriedades:** `tempoUltimoAtaque`
*   **Métodos:**
    *   `atacar()`: Instancia um `ProjetilMagico`.
    *   `teletransportar(novoX, novoY)`: Chamado quando o projétil mágico colide.

**Classe: `JogadorBomba` (Herda de `Lutador`)**
*   **Propriedades:** `tempoUltimoPlantio`
*   **Métodos:**
    *   `atacar()`: Instancia um objeto `Bomba` na posição atual.

## 3. Elementos Gerados (Composição)

**Classe: `Projetil` (Herda de `EntidadeJogo`)**
*   **Propriedades:** `dano`, `Lutador dono`
*   **Métodos:** `colidirCom(Lutador alvo)`

**Classe: `Bomba` (Não se move, não herda de EntidadeJogo de movimento contínuo)**
*   **Propriedades:** `x`, `y`, `tempoDetonacao`, `raioExplosao`, `dano`
*   **Métodos:** `atualizarTempo()`, `explodir(Lista<Lutador> lutadores)`

## 4. Gerenciamento do Jogo

**Classe: `Ringue` (Composição/Agregação)**
*   **Propriedades:** `largura`, `altura`, `lutador1`, `lutador2`, `listaProjeteis`, `listaBombas`
*   **Métodos:** `atualizarFisica()`, `detectarColisoes()`, `desenharTela()`

**Classe: `Arbitro` (Referee)**
*   **Propriedades:** `estadoPartida`, `tempoDecorrido`
*   **Métodos:** `iniciarLuta()`, `verificarVencedor()`, `finalizarPartida()`
