const tcpp = require('tcp-ping');
const pify = require('pify');

const { port, domain } = require('./config');

module.exports = {
    description: `Probing ${domain} on port ${port}...`,
    task: () =>
        pify(tcpp.probe, { multiArgs: true })(domain, port).then(result => {
            if (!result[0]) {
                throw new Error(
                    `Cannot reach ${domain}:${port} - is your server running?`
                );
            }
        }),
};
