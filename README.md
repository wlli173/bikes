Willighan:
1. Automação e Pipeline de CI/CD (Nuvem/GitHub) 
Criar uma esteira automatizada utilizando GitHub Actions (ou outra ferramenta de CI/CD como GitLab CI). 

A pipeline deve ser disparada automaticamente a cada push ou pull request na branch principal (main / master) e deve conter as seguintes etapas: 

    Compilação e Testes: Execução automatizada dos testes unitários/integração da aplicação Spring. 
    Build da Imagem Docker: Construção da imagem utilizando o Multistage Build. 
    Publicação (Push): Envio da imagem gerada para um Container Registry (Docker Hub, GitHub Container Registry - GHCR, etc.). 
    
Lucas:
2. Monitoramento Local (Prometheus ou similar) 
A aplicação Java Spring deve expor métricas através do Spring Boot Actuator e Micrometer (/actuator/prometheus). 

No ambiente local, deve subir uma ferramenta de monitoramento (ex: Prometheus) configurada para realizar a coleta (scrape) contínua das métricas expostas pela aplicação. 

3. Observabilidade e Dashboards (Grafana)
Subir uma instância do Grafana integrada ao Prometheus como fonte de dados (Data Source). 

Criar e disponibilizar um Dashboard customizado com gráficos exibindo métricas do sistema e da aplicação Java Spring, tais como: 
 
Consumo de Memória Heap da JVM. 
Uso de CPU da aplicação. 
Taxa de requisições HTTP por segundo (RPS). 
Contagem e tempo de resposta de requisições por status code (2xx, 4xx, 5xx). 

Victor:
Requisito Obrigatório — Dockerfile com Multistage Build
Estágio 1 (build) com JDK/Maven e Estágio 2 (runtime) com JRE enxuto.

4. Gestão Centralizada de Logs (Graylog)
Subir a infraestrutura completa do Graylog (MongoDB + OpenSearch) e configurar a aplicação Java Spring (Logback com appender GELF) para enviar todos os logs em formato estruturado, garantindo que mensagens de erro, info e debug estejam visíveis e pesquisáveis no painel do Graylog.

5. Orquestração Única (docker-compose.yml)
Integrar toda a infraestrutura local (App, Prometheus, Grafana, Graylog, MongoDB, OpenSearch) para executar com um único comando `docker compose up --build`, sem nenhuma intervenção manual.

6. Documentação e Evidências (README.md)
Instruções de execução, explicação das decisões técnicas e prints do pipeline CI/CD, dos dashboards do Grafana e dos logs no Graylog.
