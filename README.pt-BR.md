# Revision Validity Index

Versão de desenvolvimento **0.1.0**, da **Lofrano Tecnologia**. Licença MIT.

Biblioteca Kotlin/JVM em memória para rótulos e fatos de configuração corrigíveis. O intervalo efetivo responde **quando o valor se aplica**; a revisão lógica responde **o que o processo sabia após uma alteração aceita**. Revisões não são datas, prova de autoria nem registros de auditoria duráveis.

- Organização: https://lofranotecnologia.com.br
- Repositório: https://github.com/martinlofranodeoliveira/revision-validity-index
- [English](README.md)
- [Contrato preciso](docs/contract.md), [projeto](docs/design.md), [contagem dos limites](docs/limits.md)

## Instalação e verificação

Use exatamente **Gradle 8.10.2 e JDK 21**, por exemplo a imagem de desenvolvimento especificada `gradle:8.10.2-jdk21`. Não há binário de wrapper nem instalador. O build fixa Kotlin JVM 2.0.21 e JUnit Jupiter 5.11.4. JUnit é usado apenas nos testes. Em execução são necessários JDK 21 e a biblioteca padrão Kotlin, sem banco, modelo, API ou serviço externo.

Mantenha caches, arquivos do usuário Java e estado do compilador em `build/`, que é ignorado:

```sh
mkdir -p build/gradle-home build/java-home build/tmp
export GRADLE_USER_HOME="$PWD/build/gradle-home"
export JAVA_TOOL_OPTIONS="-Duser.home=$PWD/build/java-home -Djava.io.tmpdir=$PWD/build/tmp -Dfile.encoding=UTF-8"
```

`gradle.lockfile` e `gradle/verification-metadata.xml` são fontes obrigatórias de integridade. Devem conter locks e hashes SHA-256 obtidos de resolução real, inclusive dos artefatos de metadados. A ausência desses arquivos interrompe o build. Consulte [docs/integrity.md](docs/integrity.md) para preparação inicial, revisão e verificação com cache vazio. Não invente registros nem desative a verificação.

Com os registros revisados, resolva as dependências uma vez com rede:

```sh
gradle --dependency-verification strict resolveDependencies testClasses
```

Depois execute as duas verificações offline separadamente:

```sh
gradle --offline test
gradle --offline --quiet demoAssert
```

A saída padrão exata da demonstração é uma linha:

```text
DEMO_OK old=blue corrected=green removed=missing
```

A linha é legível e também pode ser comparada por scripts. O Java pode informar `JAVA_TOOL_OPTIONS` em stderr. Antes de imprimir sucesso, a demonstração consulta o histórico e verifica uma tentativa de alteração da visão exportada. O CI resolve dependências com registros revisados e executa testes e demonstração offline em etapas separadas; não gera registros de integridade nem publica artefatos.

Para gerar um JAR local:

```sh
gradle --offline jar
```

O arquivo é `build/libs/revision-validity-index-0.1.0.jar`, sem dependências embutidas. A aplicação que o incorporar deve fornecer Kotlin stdlib 2.0.21 no classpath. Não há configuração de publicação.

## Exemplo pequeno

```kotlin
import br.com.lofranotecnologia.revision.*

val indice = RevisionValidityIndex()
val r1 = indice.put("rotulo-sintetico", 0, 10, "azul", 0)
val anterior = indice.snapshot(r1)
val r2 = indice.put("rotulo-sintetico", 4, 7, "verde", r1)
check(indice.query(anterior, "rotulo-sintetico", 5) == FactValue.Present("azul"))
check(indice.query(indice.snapshot(r2), "rotulo-sintetico", 5) == FactValue.Present("verde"))
val r3 = indice.erase("rotulo-sintetico", 6, 9, r2)
check(indice.query(indice.snapshot(r3), "rotulo-sintetico", 6) == FactValue.Missing)
```

## Intervalos e concorrência

O intervalo é semiaberto: `[from, until)`. A unidade inteira é escolhida pelo chamador; não há inferência de data. A revisão 0 é vazia e sempre consultável. Só revisões com alteração, de 1 até N, contam para retenção.

`put` divide os intervalos atingidos, substitui a faixa e une vizinhos com valores iguais. `erase` cria uma lacuna, não um valor nulo. Uma operação sem mudança ainda verifica `expectedRevision` antes da comparação, retorna a revisão atual e não consome histórico, mesmo com o limite cheio.

Um `ReentrantLock` serializa escritores. A comparação da revisão esperada e a publicação são atômicas; duas escritas que alteram o estado com a mesma expectativa não podem ambas ter sucesso. Consultas com snapshot já obtido não usam o lock de escrita. O histórico imutável é publicado por referência volátil. Mapas e listas exportados são cópias defensivas com proteção real contra alterações, inclusive via Java.

Os limites padrão são 128 revisões com alteração, 1024 entidades ativas por snapshot, 256 intervalos por entidade e 65536 referências de intervalo retidas. Não há poda automática. IDs não vazios aceitam no máximo 128 bytes UTF-8; valores aceitam até 4096 bytes e podem ser vazios. Surrogates isolados são rejeitados, não substituídos. Não há normalização Unicode.

## Motivação e limitações

Snapshots completos e imutáveis tornam consultas históricas e falhas de capacidade simples de inspecionar. O custo é copiar coleções durante a seção crítica do escritor. Os limites são lógicos, não uma medição exata do heap. Não se afirma desempenho medido nem prontidão para produção.

Os testes incluem limites de `Long`, Unicode, imutabilidade profunda, preservação de snapshots, igualdade dos limites e rejeições sem efeitos. As disputas entre escritores usam barreira, sem sleeps como critério. Um oráculo independente mantém células e revisões para comparar correções geradas por semente fixa.

Todo estado é perdido ao encerrar o processo. Ao atingir um limite de retenção ou referências, novas alterações podem ser bloqueadas, inclusive remoções. Não há autenticação, autorização, auditoria durável, suporte a registros financeiros ou clínicos, replicação, calendário, parser, serviço de rede ou poda automática. O projeto implementa apenas o contrato documentado, sem alegar conformidade ampla com padrões de bancos temporais.

Relatos de segurança seguem [SECURITY.md](SECURITY.md). Para contato geral, use a [página oficial](https://lofranotecnologia.com.br).
