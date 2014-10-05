var catanApp = angular.module("CatanApp", [], function() {});

var baseUrl = location.host;
var webSocketUrl = 'ws://' + baseUrl + '/register';
var baseHttpUrl = 'http://' + baseUrl;
var registerUrl = baseHttpUrl + '/register';
var startGameUrl = baseHttpUrl + '/start_game';
var newBoardUrl = baseHttpUrl + '/new_game';
var actionSubmitUrl = baseHttpUrl + '/action';
var tradeRequestUrl = baseHttpUrl + '/trade';

var SQRT_3_INV = 1 / Math.pow(3, 0.5);
var hexWidth = 100;
var circleRadius = hexWidth / 5;
var circleColor = 'tan';
var boardPadding = 16;
var fontSize = 15;

var PING_INTERVAL = 1500;
var PONG_WAIT = 250;

// TODO: change, obvs
var fixedUserId = "steven";

function buildProbDots(rollNumber) {
    if (!rollNumber)
        return '';
    var numDots = 6 - Math.abs(7 - rollNumber);
    var dots = '';
    for (var i=0; i < numDots; i++)
        dots += '.';
    return dots;
}

function pointsArrayFromObj(pointsObj) {
    var points = [];
    angular.forEach(['topLeft', 'top', 'topRight', 'bottomRight', 'bottom', 'bottomLeft'], function(attr) {
        points.push(pointsObj[attr]);
    });
    return points;
}

function getIntersectionsAsArray(intersectionData) {
    var retArray = [];
    angular.forEach(['topLeft', 'top', 'topRight', 'bottomRight', 'bottom', 'bottomLeft'], function(attr) {
        retArray.push(intersectionData[attr]);
    });
    return retArray;
}

function makeSVGPointStr(points) {
    var pointString = '';
    angular.forEach(points, function(point) {
        pointString += point.x + ',' + point.y + ' '; // trailing space shouldn't matter
    });
    return pointString;
}

function makeHexSVGPoints(points) {
    var pointArray = getIntersectionsAsArray(points);
    return makeSVGPointStr(pointArray);
}

function findScaledMidPoint(point1, point2, scalar) {
    var xVal = (point1.x + point2.x) * scalar;
    var yVal = (point1.y + point2.y) * scalar;
    return {x: xVal, y: yVal};
}

function findMidPoint(point1, point2) {
    return findScaledMidPoint(point1, point2, 0.5);
}

function makeSettlementSVGPoints(centerX, centerY, scalar, isCity) {
    if (!scalar)
        scalar = 1;
    scalar *= 0.3;
    var offset = scalar * 0.5;
    var points = [{x: centerX - offset, y: centerY + offset},
                  {x: centerX - offset, y: centerY - offset},
                  {x: centerX, y: centerY - offset * 2},
                  {x: centerX + offset, y: centerY - offset}];
    if (isCity) {
        points.push({x: centerX + offset * 2.5, y: centerY - offset});
        points.push({x: centerX + offset * 2.5, y: centerY + offset});
    }
    else {
        points.push({x: centerX + offset, y: centerY + offset});
    }
    points.push({x: centerX + offset, y: centerY + offset});
    return makeSVGPointStr(points);
}

catanApp.factory('catanBackend', ['$http', function($http) {
    var catanBackend = {};
    catanBackend.pongReceived = 0;
    catanBackend.pongStart = 0;
    var getState = function() {
        return $http.get(registerUrl).error(function(err) { alert(JSON.stringify(err)); });
    };
    var startGame = function(boardId, userId) {
        return $http.post(startGameUrl + '?' + $.param({board_id: boardId, user_id: userId})).error(function(err) { alert(JSON.stringify(err)); });
    };
    var connectWebSocket = function(boardId, userId, handleMessageCallback) {
        console.log("Connecting new websocket");
        var ws = new WebSocket(webSocketUrl + '?' + $.param({board_id: boardId, user_id: userId}), ["protocolOne", "protocolTwo"]);
        ws.onmessage = function(messageEvt) {
            message = messageEvt.data;
            console.log("Got message: " + message);
            if (message == "pong!") {
                catanBackend.pongReceived = Date.now();
                return;
            }
            // Messages coming back in websocket with weird trailing chars, just get rid for now.
            var temp = message.replace(/[^\w \d}{\'\"\-:\,\[\]]+/gi,'');
            var jsonMessage = JSON.parse(temp);
            return handleMessageCallback(jsonMessage);
        };
        ws.onopen = function() {
            pingServer();
            // For now, just start the game right away.
            startGame(boardId, userId);
        };
        catanBackend.webSocket = ws;
        return ws;
    };
    var pingServer = function() {
        catanBackend.pongStart = Date.now();
        try {
            catanBackend.webSocket.send("ping!");
        } catch(e) {
            return connectWebSocket();
        }
        setTimeout(receivePong, PONG_WAIT);
    };
    var receivePong = function() {
        if (catanBackend.pongReceived < catanBackend.pongStart) {
            console.log("WS fail.");
            return connectWebSocket();
        }
        setTimeout(pingServer, PING_INTERVAL - PONG_WAIT);
    };
    var sendMessage = function (message) {
        catanBackend.ws.send(message);
    };
    var newBoard = function (userId) {
        return $http.post(newBoardUrl + '?' + $.param({user_id: userId})).error(function(err) { alert(JSON.stringify(err)); });
    };

    catanBackend.getState = getState;
    catanBackend.sendMessage = sendMessage;
    catanBackend.connectWebSocket = connectWebSocket;
    catanBackend.newBoard = newBoard;
    return catanBackend;
}]);

catanApp.controller('BoardController', ['$scope', 'catanBackend', function($scope, catanBackend) {
    var boardCache;
    $scope.userId = fixedUserId;
    $scope.board = {};
    $scope.intersections = [];
    $scope.hexes = [];
    $scope.gameMessages = [];
    $scope.hexColorMap = {
        ocean: '#0099FF',
        hill: '#AD5C33',
        field: '#FFCC00',
        forest: '#197519',
        mountain: 'grey',
        desert: '#FFFFC2',
        pasture: '#83FF83' // kind of light greeny
    };
    $scope.resourceHexMap = {
        brick: 'hill',
        sheep: 'pasture',
        wood: 'forest',
        ore: 'mountain',
        wheat: 'field'
    };

    var getResourceColor = function(resource) {
        var terrain = $scope.resourceHexMap[resource];
        return $scope.hexColorMap[terrain] || '#E6E6E6';
    };

    var refreshAll = function(response) {
        var board = response.data.board;
        $scope.playerInfo = board.playerInfo;
        refreshHand(board.playerHand);
        boardCache = board;
        refreshHexes(board);
        refreshPieces(board);
    };

    var refreshHand = function(hand) {

    };

    var refreshHexes = function(board) {
        // TODO: BREAK THIS UP!
        // board.hexes.hexes is 2-d array
        var hexDiameter = hexWidth * 2 * SQRT_3_INV,
            sideLength = hexDiameter / 2,
            hexExtra = sideLength / 2,
            hexAmortizedHeight = sideLength + hexExtra;
        var rowDecrements = 0;
        var scopeHexes = [],
            scopeCircles = [],
            scopeIntersections = [],
            scopeEdges = [],
            harborLines = [],
            robberPoints = '';

        var hexGrid = board.hexes.hexes;
        angular.forEach(hexGrid, function(hexRow, i) {
            var allNulls = true;
            var trueIndex = i - rowDecrements,
                sideOffset = (trueIndex % 2) ? 0 : hexWidth / 2,
                yStart = trueIndex * hexAmortizedHeight;
            angular.forEach(hexRow, function(hex, j) {
                if (!hex)
                    return;
                allNulls = false;
                var xStart = sideOffset + j * hexWidth;
                var points = {
                    topLeft: {x: xStart, y: yStart + hexExtra},
                    top: {x: xStart + hexWidth / 2, y: yStart},
                    topRight: {x: xStart + hexWidth, y: yStart + hexExtra},
                    bottomRight: {x: xStart + hexWidth, y: yStart + hexDiameter - hexExtra},
                    bottom: {x: xStart + hexWidth / 2, y: yStart + hexDiameter},
                    bottomLeft: {x: xStart, y: yStart + hexDiameter - hexExtra}
                };
                scopeHexes.push({
                    points: makeHexSVGPoints(points),
                    color: $scope.hexColorMap[hex.hexType]
                });
                var centerPoint = findMidPoint(points.bottom, points.top);
                var dots = buildProbDots(hex.rollNumber);
                if (hex.rollNumber) {
                    scopeCircles.push({
                        cx: centerPoint.x,
                        cy: centerPoint.y,
                        textX: centerPoint.x - fontSize * 0.25 * String(hex.rollNumber).length,
                        textY: centerPoint.y - fontSize * 0.05,
                        dotsX: centerPoint.x - fontSize * 0.13 * dots.length,
                        dotsY: centerPoint.y + fontSize * 0.35,
                        radius: circleRadius,
                        color: circleColor,
                        fontSize: fontSize + 'px',
                        text: hex.rollNumber,
                        // text: hex.xPosition + ',' + hex.yPosition + ',' + (-(hex.yPosition + hex.xPosition)),
                        probabilityDots: dots
                    });
                }
                else if (hex.harborType) {
                    scopeCircles.push({
                        cx: centerPoint.x,
                        cy: centerPoint.y,
                        // text: hex.xPosition + ',' + hex.yPosition + ',' + (-(hex.yPosition + hex.xPosition)),
                        radius: circleRadius,
                        textX: centerPoint.x - fontSize * 0.25,
                        textY: centerPoint.y - fontSize * 0.05,
                        text: hex.harborType.toLowerCase() == 'generic' ? '?' : '',
                        color: getResourceColor(hex.harborType.toLowerCase())
                    });
                }
                if (hex.id == board.hexes.robberHexId) {
                    var robberPointArr = [points.top, points.bottomLeft, points.bottomRight];
                    robberPointArr = robberPointArr.map(function(p) {
                        return findMidPoint(centerPoint, p);
                    });
                    robberPoints = makeSVGPointStr(robberPointArr);
                }
                var interArray = getIntersectionsAsArray(points);
                for (var index=0; index<6; index++) {
                    // this uniquely covers all relevant intersections on board
                    var interId = hex.intersections.ids[index];
                    var interInfo = board.intersections.intersections[interId];
                    var interCenterPoint = interArray[index];
                    if (hex.harborType && interInfo && interInfo.harborType) {
                        harborLines.push({
                            x1: centerPoint.x,
                            y1: centerPoint.y,
                            x2: interCenterPoint.x,
                            y2: interCenterPoint.y
                        });
                    }
                    // Only draw other info 1 time per intersection.
                    if (index > 1)
                        continue;
                    if (interInfo && interInfo.color) {
                        scopeIntersections.push({
                            color: interInfo.color.toLowerCase(),
                            points: makeSettlementSVGPoints(interCenterPoint.x, interCenterPoint.y, sideLength, interInfo.isCity)
                        });
                    }
                }
                angular.forEach([5, 0, 1], function(index) {
                    // this uniquely covers all relevant edges on board
                    var edgeId = hex.edges.ids[index];
                    var edgeInfo = board.edges.edges[edgeId];
                    if (edgeInfo && edgeInfo.color) {
                        var point1 = interArray[(index + 5) % 6];
                        var point2 = interArray[(index + 6) % 6];
                        scopeEdges.push({
                            color: edgeInfo.color.toLowerCase(),
                            x1: point1.x,
                            x2: point2.x,
                            y1: point1.y,
                            y2: point2.y
                        });
                    }
                });
            });
            if (allNulls)
                rowDecrements++;
        });

        $scope.board.height = (hexGrid.length - rowDecrements) * hexAmortizedHeight + hexExtra + boardPadding;
        $scope.board.width = (hexGrid[0].length + 1) * hexWidth + boardPadding;
        $scope.hexes = scopeHexes;
        $scope.circles = scopeCircles;
        $scope.intersections = scopeIntersections;
        $scope.harborLines = harborLines;
        $scope.edges = scopeEdges;
        $scope.robberPoints = robberPoints;
    };

    var refreshPieces = function(board) {

    };

    var handleMessage = function(message) {
        var gameMessage = messageActions[message.eventType](message);
        $scope.gameMessages.push(gameMessage);
        $scope.$apply();

        // not sure how to do this in angular
        setTimeout(
            function() {
                $(".game-message-container").scrollTop($(".game-message-container")[0].scrollHeight);
            },
            25
        );
    };

    var messageActions = {
        TURN_SETUP_START: function(message) {
            var player = message.player;
            return 'Player ' + player.color + ' beginning setup turn.';
        },
        TURN_START: function(message) {
            var player = message.player;
            return 'Player ' + player.color + ' beginning turn.';
        },
        INTERSECTION_CHANGE: function(message) {
            var intersection = message.intersection;
            boardCache.intersections.intersections[intersection.id] = intersection;
            refreshHexes(boardCache);
            return 'Player ' + intersection.color + ' built a new ' + (intersection.isCity ? 'city' : 'settlement');
        },
        EDGE_CHANGE: function(message) {
            var edge = message.edge;
            boardCache.edges.edges[edge.id] = edge;
            refreshHexes(boardCache);
            return 'Player ' + edge.color + ' built a new road';
        },
        TRADE_EVENT: function(message) {
            // TODO: anything if this was with the current player
            return 'A trade happened!';
        },
        DEV_CARD_PULL_EVENT: function(message) {
            var player = message.player;
            return 'Player ' + player.color + " bought a development card.";
        },
        WIN_EVENT: function(message) {
            var player = message.player;
            return 'Player ' + player.color + " has won the game!";
        }
    };

    var registerAndWait = function(board) {
        catanBackend.connectWebSocket(board.id, $scope.userId, handleMessage);
    };

    var doMyTurn = function(response) {

    };

    // refreshHexes(fakeBoard.board);

    // catanBackend.getState().then(function(response) {
    //     refreshAll(response);
    //     waitForMyTurn(response);
    // });

    catanBackend.newBoard($scope.userId).then(function(response) {
        refreshAll(response);
        registerAndWait(response.data.board);
    });
}]);
