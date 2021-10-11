# Guião de Demonstração (Parte I)

## 1. Preparação do Sistema

Para testar a aplicação e todos os seus componentes, é necessário preparar um ambiente com dados para proceder à verificação dos testes.

### 1.1. Compilar o Projeto

Primeiramente, é necessário instalar as dependências necessárias para o *silo* e os clientes (*eye* e *spotter*) e compilar estes componentes.
Para isso, basta ir à diretoria *root* do projeto e correr o seguinte comando:

```
$ mvn clean install -DskipTests
```

Com este comando já é possível analisar se o projeto compila na íntegra.

### 1.2. *Silo*

Para proceder aos testes, é preciso o servidor *silo* estar a correr. 
Para isso basta ir à diretoria *silo-server* e executar:

```
$ mvn compile exec:java
```

Este comando vai colocar o *silo* no endereço *localhost* e na porta *8080*.

### 1.3. *Eye*

Vamos registar 3 câmeras e as respetivas observações. 
Cada câmera vai ter o seu ficheiro de entrada próprio com observações já definidas.
Para isso basta ir à diretoria *eye* e correr os seguintes comandos:

```
$ mvn compile exec:java -Deye.name=Tagus -Deye.latitude=38.737613 -Deye.longitude=-9.303164 < ../demo1/eye1.txt
$ mvn compile exec:java -Deye.name=Tagus -Deye.latitude=38.737613 -Deye.longitude=-9.303164 < ../demo1/eye2.txt
$ mvn compile exec:java -Deye.name=Tagus -Deye.latitude=38.737613 -Deye.longitude=-9.303164 < ../demo1/eye3.txt
```
**Nota:** Para correr o script *eye* é necessário fazer `mvn install` e adicionar ao *PATH* ou utilizar diretamente os executáveis gerados na diretoria `target/appassembler/bin/`.

Depois de executar os comandos acima já temos o que é necessário para testar o sistema. 

## 2. Teste das Operações

Nesta secção vamos correr os comandos necessários para testar todas as operações. 
Cada subsecção é respetiva a cada operação presente no *silo*.

### 2.1. *cam_join*

Esta operação já foi testada na preparação do ambiente, no entanto ainda é necessário testar algumas restrições.

2.1.1 Teste de camaras com o mesmo nome e diferentes coordenadas.
O servidor deve rejeitar esta operação. 
Para isso basta executar um *eye* com o seguinte comando:

```
$ mvn compile exec:java -Deye.name=Tagus -Deye.latitude=10.0 -Deye.longitude=10.0
```

2.1.2. Teste do tamanho do nome.  
O servidor deve rejeitar esta operação. 
Para isso basta executar um *eye* com o seguinte comando:

```
$ mvn compile exec:java -Deye.name=ab -Deye.latitude=38.737613 -Deye.longitude=-9.303164
$ mvn compile exec:java -Deye.name=abcdefghijklmnop -Deye.latitude=38.737613 -Deye.longitude=-9.303164
```

### 2.2. *cam_info*

Esta operação não está disponivel diretamente.

### 2.3. *report*

Esta operação já foi testada acima na preparação do ambiente.

No entanto falta testar o sucesso do comando *zzz*. 
Na preparação foi adicionada informação que permite testar este comando.
Para testar basta abrir um cliente *spotter* e correr o comando seguinte:

```
> trail car 00AA00
```

O resultado desta operação deve ser duas observações pela câmera *Tagus*.

### 2.4. *track*

Esta operação vai ser testada utilizando o comando *spot* com um identificador.

2.4.1. Teste com uma pessoa (deve devolver vazio):

```
> spot person 14388236
```

2.4.2. Teste com uma pessoa:

```
> spot person 123456789
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
```

2.4.3. Teste com um carro:

```
> spot car 20SD21
car,20SD21,<timestamp>,Alameda,30.303164,-10.737613
```

### 2.5. *trackMatch*

Esta operação vai ser testada utilizando o comando *spot* com um fragmento de identificador.

2.5.1. Teste com uma pessoa (deve devolver vazio):

```
> spot person 143882*
```

2.5.2. Testes com uma pessoa:

```
> spot person 111*
person,111111000,<timestamp>,Tagus,38.737613,-9.303164

> spot person *000
person,111111000,<timestamp>,Tagus,38.737613,-9.303164

> spot person 111*000
person,111111000,<timestamp>,Tagus,38.737613,-9.303164
```

2.5.3. Testes com duas ou mais pessoas:

```
> spot person 123*
person,123111789,<timestamp>,Alameda,30.303164,-10.737613
person,123222789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613

> spot person *789
person,123111789,<timestamp>,Alameda,30.303164,-10.737613
person,123222789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613

> spot person 123*789
person,123111789,<timestamp>,Alameda,30.303164,-10.737613
person,123222789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
```

2.5.4. Testes com um carro:

```
> spot car 00A*
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164

> spot car *A00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164

> spot car 00*00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
```

2.5.5. Testes com dois ou mais carros:

```
> spot car 20SD*
car,20SD20,<timestamp>,Alameda,30.303164,-10.737613
car,20SD21,<timestamp>,Alameda,30.303164,-10.737613
car,20SD22,<timestamp>,Alameda,30.303164,-10.737613

> spot car *XY20
car,66XY20,<timestamp>,Lisboa,32.737613,-15.303164
car,67XY20,<timestamp>,Alameda,30.303164,-10.737613
car,68XY20,<timestamp>,Tagus,38.737613,-9.303164

> spot car 19SD*9
car,19SD19,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD29,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD39,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD49,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD59,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD69,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD79,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD89,<timestamp>,Lisboa,32.737613,-15.303164
car,19SD99,<timestamp>,Lisboa,32.737613,-15.303164
```

### 2.6. *trace*

Esta operação vai ser testada utilizando o comando *trail* com um identificador.

2.6.1. Teste com uma pessoa (deve devolver vazio):

```
> trail person 14388236
```

2.6.2. Teste com uma pessoa:

```
> trail person 123456789
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Alameda,30.303164,-10.737613
person,123456789,<timestamp>,Tagus,38.737613,-9.303164

```

2.6.3. Teste com um carro (deve devolver vazio):

```
> trail car 12XD34
```

2.6.4. Teste com um carro:

```
> trail car 00AA00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
```

## 3. Considerações Finais

Apesar de não serem avaliados os comandos de controlo, o comando *help* deve ser minimamente informativo e deve indicar todas as operações existentes no *spotter*.
Estes testes não cobrem tudo, pelo que devem ter sempre em conta os testes de integração e o código.

# Guião de Demonstração (Parte II)

### 1.1. Compilar o Projeto

Esta etapa é a mesma que a Parte I.

## 2. Testes com varios servidores
 
### 2.1. Dois servidores distintos e dois clientes - Spotter e Eye.

Nesta secção vamos usar as operações de report e trail para demonstrar o comportamento de dois clientes, um spotter e um eye ligados cada um a um servidor diferente.É necessário ter o Zookeeper a correr.

2.1.1. Lançar dois servidores distintos com gossip timers de 10 segundos
 
Executar os seguintes comandos:

```
$ mvn compile exec:java -Dinstance=1 -DtimeToSend=10
$ mvn compile exec:java -Dinstance=2 -DtimeToSend=10
```

2.1.2 Lançar dois clientes ligados a servidores distintos

Executar o seguinte comando para ligar o cliente eye ao servidor instance=1:

```
$ mvn compile exec:java -Dserver.instance=1 -Deye.name=Tagus -Deye.latitude=38.737613 -Deye.longitude=-9.303164
```

Executar o seguinte comando para ligar o cliente spotter ao servidor instance=2:

```
$ mvn compile exec:java -Dserver.instance=2
```

2.1.3 Reportar um carro no server 1 pelo cliente eye

```
> car,00AA00
```

2.1.4 Fazer trail do carro reportado pelo cliente 1 no cliente spotter que está ligado ao server 2, passados 10 segundos:

```
> trail car 00AA00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
```

### 2.2. Um servidor que vai abaixo e dois clientes - Spotter e Eye.
Nesta secção vamos usar as operações de report e trail para demonstrar o comportamento de dois clientes, um spotter e um eye ligados ao mesmo server quando este vai abaixo.

2.2.1 Lançar um servidor

Executar o seguinte comando para ligar o servidor:

```
$ mvn compile exec:java
```

2.2.2 Lançar dois clientes ligados ao mesmo

Executar o seguinte comando para ligar o cliente eye ao servidor:

```
$ mvn compile exec:java -Deye.name=Tagus -Deye.latitude=38.737613 -Deye.longitude=-9.303164
```

Executar o seguinte comando para ligar o cliente spotter ao servidor:

```
$ mvn compile exec:java
```

2.2.3 Reportar o carro no server pelo cliente eye

```
> car,00AA00
```

2.2.4 Fazer trail do carro reportado pelo cliente eye:

```
> trail car 00AA00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
```

2.2.5 Matar o servidor (ctrl + c)

2.2.6 Fazer trail do carro reportado pelo cliente eye:

```
> trail car 00AA00
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
```
