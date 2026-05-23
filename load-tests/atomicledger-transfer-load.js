import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const apiKey = __ENV.API_KEY || 'atomicledger-local-api-key';
const walletCurrency = __ENV.CURRENCY || 'INR';
const sourceWalletId = __ENV.SOURCE_WALLET_ID || '';
const destinationWalletId = __ENV.DESTINATION_WALLET_ID || '';
const ownerPrefix = __ENV.OWNER_PREFIX || 'k6-wallet';
const initialDepositAmount = Number(__ENV.INITIAL_DEPOSIT_AMOUNT || '100000');
const depositAmount = Number(__ENV.DEPOSIT_AMOUNT || '250');
const transferAmount = Number(__ENV.TRANSFER_AMOUNT || '5');
const depositEvery = Number(__ENV.DEPOSIT_EVERY || '20');
const sleepSeconds = Number(__ENV.SLEEP_SECONDS || '0.2');

export const options = {
	scenarios: {
		transfer_load: {
			executor: __ENV.EXECUTOR || 'constant-vus',
			vus: Number(__ENV.VUS || '5'),
			duration: __ENV.DURATION || '30s',
		},
	},
	thresholds: {
		http_req_failed: ['rate<0.05'],
		http_req_duration: ['p(95)<1000'],
	},
};

function apiHeaders(extraHeaders = {}) {
	return {
		'Content-Type': 'application/json',
		Accept: 'application/json',
		'X-API-Key': apiKey,
		...extraHeaders,
	};
}

function createWallet(ownerReference) {
	const response = http.post(
		`${baseUrl}/api/v1/wallets`,
		JSON.stringify({
			ownerReference,
			currency: walletCurrency,
		}),
		{ headers: apiHeaders() }
	);

	const success = check(response, {
		'wallet create returned 201': res => res.status === 201,
		'wallet create returned wallet id': res => !!res.json('id'),
	});
	if (!success) {
		fail(`wallet creation failed: ${response.status} ${response.body}`);
	}
	return response.json('id');
}

function deposit(walletId, amount, idempotencyKey) {
	const response = http.post(
		`${baseUrl}/api/v1/wallets/${walletId}/deposit`,
		JSON.stringify({
			amount,
			currency: walletCurrency,
		}),
		{
			headers: apiHeaders({
				'Idempotency-Key': idempotencyKey,
			}),
		}
	);

	check(response, {
		'deposit returned 201': res => res.status === 201,
		'deposit response has transaction id': res => !!res.json('transactionId'),
	});
	return response;
}

function transfer(sourceId, destinationId, amount, idempotencyKey) {
	return http.post(
		`${baseUrl}/api/v1/transfers`,
		JSON.stringify({
			sourceWalletId: sourceId,
			destinationWalletId: destinationId,
			amount,
			currency: walletCurrency,
		}),
		{
			headers: apiHeaders({
				'Idempotency-Key': idempotencyKey,
			}),
		}
	);
}

function iterationId() {
	return `${exec.vu.idInTest}-${exec.scenario.iterationInTest}`;
}

export function setup() {
	let sourceId = sourceWalletId;
	let destinationId = destinationWalletId;

	if ((sourceId && !destinationId) || (!sourceId && destinationId)) {
		fail('SOURCE_WALLET_ID and DESTINATION_WALLET_ID must be provided together');
	}

	if (!sourceId && !destinationId) {
		sourceId = createWallet(`${ownerPrefix}-source`);
		destinationId = createWallet(`${ownerPrefix}-destination`);
	}

	const seedKey = `k6-seed-${Date.now()}`;
	const seedResponse = deposit(sourceId, initialDepositAmount, seedKey);
	const seedOk = check(seedResponse, {
		'initial seed deposit succeeded': res => res.status === 201,
	});
	if (!seedOk) {
		fail(`initial deposit failed: ${seedResponse.status} ${seedResponse.body}`);
	}

	return {
		sourceWalletId: sourceId,
		destinationWalletId: destinationId,
	};
}

export default function (data) {
	const currentIteration = exec.scenario.iterationInTest;
	const currentId = iterationId();

	if (depositEvery > 0 && currentIteration > 0 && currentIteration % depositEvery === 0) {
		const topUpKey = `k6-deposit-${currentId}`;
		const depositResponse = deposit(data.sourceWalletId, depositAmount, topUpKey);
		check(depositResponse, {
			'periodic deposit succeeded': res => res.status === 201,
		});
	}

	const transferKey = `k6-transfer-${currentId}`;
	const transferResponse = transfer(data.sourceWalletId, data.destinationWalletId, transferAmount, transferKey);
	check(transferResponse, {
		'transfer returned 201 or 409': res => res.status === 201 || res.status === 409,
	});

	if (currentIteration % 10 === 0) {
		const replayKey = `k6-replay-${currentId}`;
		const firstReplay = transfer(data.sourceWalletId, data.destinationWalletId, transferAmount, replayKey);
		const secondReplay = transfer(data.sourceWalletId, data.destinationWalletId, transferAmount, replayKey);

		check(firstReplay, {
			'first replay probe returned 201': res => res.status === 201,
		});
		check(secondReplay, {
			'duplicate replay returned 201': res => res.status === 201,
			'duplicate replay reused original transaction id': res =>
				res.json('transactionId') === firstReplay.json('transactionId'),
		});
	}

	sleep(sleepSeconds);
}
