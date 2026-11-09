async function container(request) {
    const id = request.variables.container_id;
    const operation = request.variables.operation;
    try {
        const response = await request.subrequest("/_docker_inspect/" + id + "/json", { args: "" });
        if (response.status === 404) {
            request.return(404);
            return;
        }
        if (response.status !== 200) {
            request.return(502);
            return;
        }

        const info = JSON.parse(response.responseText);
        const labels = info.Config.Labels || {};
        if (labels["com.docker.compose.project"] !== "sugang-helper") {
            request.return(404);
            return;
        }

        if (operation === "logs") {
            const args = request.variables.args;
            let path = "/_docker_logs/" + id + "/logs";
            if (args) {
                path += "?" + args;
            }
            request.internalRedirect(path);
            return;
        }

        request.headersOut["Content-Type"] = "application/json";
        request.return(200, JSON.stringify({
            Id: id,
            Config: { Tty: info.Config.Tty },
            State: { Running: info.State.Running, FinishedAt: info.State.FinishedAt },
        }));
    } catch (error) {
        request.return(502);
    }
}

export default { container };
