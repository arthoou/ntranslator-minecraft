<div align="center">

<img src="src/main/resources/icon.png" alt="Logo de NTranslator" width="120">

<h1>NTranslator</h1>
<p>Interface de leitura para placas e livros, com narração local no Windows.</p>
<p><strong>Minecraft 1.20.1 · Forge · Java 17</strong></p>

</div>

[Instalação](#instalação) · [Uso](#uso) · [Como funciona](#como-funciona) · [Créditos](#créditos)

## Instalação

Coloque o JAR `build/libs/ntranslator-forge-1201-2.0-beta.jar` na pasta `mods/` do cliente. Instale também **Architectury API 9.2.14** ou versão compatível. Em multiplayer, a instalação é feita apenas na sua instância.

| Minecraft | Loader | Java | Versão do mod |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.10 | 17 | 2.0-beta |

## Uso

- **Placas:** clique com o botão direito em uma placa para abrir a tela de leitura. Para usar a interação normal do jogo, clique agachado ou com uma placa na mão.
- **Livros:** abra um livro escrito ou um livro e pena. Use os controles ao lado das páginas para escolher um idioma ou acionar a leitura.
- **Leitura em voz alta:** o botão de alto-falante usa a síntese de voz local do Windows. A voz e a saída disponíveis dependem das opções instaladas no sistema.

## Como funciona

| Parte | Comportamento neste build |
| --- | --- |
| Placas | A tela de leitura captura as linhas da face selecionada e mostra o texto original quando a tradução está indisponível. |
| Livros | A interface adiciona controles de leitura e de idioma aos livros comuns e editáveis. As páginas originais continuam disponíveis. |
| Idioma | A escolha do idioma é compartilhada entre placas e livros e é salva em `config/ntranslator.json`. |
| Tradução e transcrição | O JAR atual distribui a interface de leitura e o TTS local. Nos projetos que mantêm código de LibreTranslate e transcrição, esses módulos ficam fora do build padrão de distribuição. |

## Créditos

Desenvolvimento e ports por [Arthou](https://github.com/arthoou). A integração com Architectury API, quando exigida por esta variante, usa a biblioteca como dependência.

## Compilar

```bash
java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build
```

A saída de distribuição aparece no caminho indicado em **Instalação**.
