# Fight-Club
Trabalho de POO que consiste em fazer um jogo em JAVA

-> Descrição básica do jogo:
https://www.youtube.com/shorts/gvmEGYI86XE

É um jogo de luta automático que você seleciona o seu time de lutadores que entrarão num ringue quadrado e ficarão quicando com as paredes, colidindo entre si e se atacando conforme as especificidades de cada lutador. É bem inspirado no vídeo acima.

Os conceitos de POO serão abordados da seguinte maneira:
Herança (Inheritance): Cria uma classe base abstrata chamada Lutador. Todos os personagens específicos (LancadorAdagas, Magico, JogadorBomba) herdam dessa classe. Eles automaticamente ganham as coordenadas (x, y), a velocidade e a barra de vida.

Polimorfismo (Polymorphism): O Lançador de Adagas vai sobrescrever (override) o método de colisão para que ele apenas ricocheteie sem dar dano de contato, além de sobrescrever o método de ataque para gerar projéteis. Já o Mágico sobrescreve o ataque para disparar seu projétil especial e engatilhar um teletransporte.

Encapsulamento (Encapsulation): A barra de vida não deve ser acessada diretamente por outras classes (deve ser private). Para diminuir a vida de um jogador após uma colisão ou explosão, usa-se um método público como receberDano(int valor). Isso garante que a vida nunca fique negativa sem que a própria classe do lutador faça essa checagem.

Composição e Agregação: O ringue não é um lutador, mas ele tem lutadores, projéteis e bombas. A classe do ringue será responsável por iterar sobre esses objetos a cada frame e aplicar a física de colisão.

Gerenciamento de Estado: Para orquestrar tudo isso, gerenciar o andamento da partida e verificar quando um lutador chega a zero de vida, é uma boa prática criar uma classe Arbitro (Referee). Essa classe atua como o juiz do jogo, e pode até servir como uma base sólida para expandir o projeto no futuro com uma infraestrutura de rede para multiplayer.
