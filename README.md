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
