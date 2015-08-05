var election = new Datamap({
    scope: 'usa',
    element: document.getElementById('map-percentage'),
    geographyConfig: {
        highlightBorderColor: '#bada55',
        popupTemplate: function(geography, data) {
            return '<div class="hoverinfo">' + geography.properties.name + 'Percentage:' +  data.per + ' '
        },
        highlightBorderWidth: 3
    },

    fills: {
        'Republican': '#CC4731',
        'Democrat': '#306596',
        'Heavy Democrat': '#667FAF',
        'Light Democrat': '#A9C0DE',
        'Heavy Republican': '#CA5E5B',
        'Light Republican': '#EAA9A8',
        defaultFill: '#EDDC4E'
    },
    data:{
        "AZ": {
            "fillKey": "Republican",
            "per": 5
        },
        "CO": {
            "fillKey": "Light Democrat",
            "per": 5
        },
        "DE": {
            "fillKey": "Democrat",
            "per": 1
        },
        "FL": {
            "fillKey": "UNDECIDED",
            "per": 1
        },
        "GA": {
            "fillKey": "Republican",
            "per": 1
        },
        "HI": {
            "fillKey": "Democrat",
            "per": 1
        },
        "ID": {
            "fillKey": "Republican",
            "per": 2
        },
        "IL": {
            "fillKey": "Democrat",
            "per": 5
        },
        "IN": {
            "fillKey": "Republican",
            "per": 10
        },
        "IA": {
            "fillKey": "Light Democrat",
            "per": 1
        },
        "KS": {
            "fillKey": "Republican",
            "per": 2
        },
        "KY": {
            "fillKey": "Republican",
            "per": 1
        },
        "LA": {
            "fillKey": "Republican",
            "per": 0
        },
        "MD": {
            "fillKey": "Democrat",
            "per": 0
        },
        "ME": {
            "fillKey": "Democrat",
            "per": 0
        },
        "MA": {
            "fillKey": "Democrat",
            "per": 25
        },
        "MN": {
            "fillKey": "Democrat",
            "per": 5
        },
        "MI": {
            "fillKey": "Democrat",
            "per": 0
        },
        "MS": {
            "fillKey": "Republican",
            "per": 0
        },
        "MO": {
            "fillKey": "Republican",
            "per": 0
        },
        "MT": {
            "fillKey": "Republican",
            "per": 0
        },
        "NC": {
            "fillKey": "Light Republican",
            "per": 0
        },
        "NE": {
            "fillKey": "Republican",
            "per": 2
        },
        "NV": {
            "fillKey": "Heavy Democrat",
            "per": 10
        },
        "NH": {
            "fillKey": "Light Democrat",
            "per": 0
        },
        "NJ": {
            "fillKey": "Democrat",
            "per": 0
        },
        "NY": {
            "fillKey": "Democrat",
            "per": 0
        },
        "ND": {
            "fillKey": "Republican",
            "per": 0
        },
        "NM": {
            "fillKey": "Democrat",
            "per": 3
        },
        "OH": {
            "fillKey": "UNDECIDED",
            "per": 0
        },
        "OK": {
            "fillKey": "Republican",
            "per": 2
        },
        "OR": {
            "fillKey": "Democrat",
            "per": 3
        },
        "PA": {
            "fillKey": "Democrat",
            "per": 0
        },
        "RI": {
            "fillKey": "Democrat",
            "per": 0
        },
        "SC": {
            "fillKey": "Republican",
            "per": 0
        },
        "SD": {
            "fillKey": "Republican",
            "per": 0
        },
        "TN": {
            "fillKey": "Republican",
            "per": 0
        },
        "TX": {
            "fillKey": "Republican",
            "per": 0
        },
        "UT": {
            "fillKey": "Republican",
            "per": 1
        },
        "WI": {
            "fillKey": "Democrat",
            "per": 1
        },
        "VA": {
            "fillKey": "Light Democrat",
            "per": 1
        },
        "VT": {
            "fillKey": "Democrat",
            "per": 0
        },
        "WA": {
            "fillKey": "Democrat",
            "per": 0
        },
        "WV": {
            "fillKey": "Republican",
            "per": 0
        },
        "WY": {
            "fillKey": "Republican",
            "per": 0
        },
        "CA": {
            "fillKey": "Democrat",
            "per": 0
        },
        "CT": {
            "fillKey": "Democrat",
            "per": 0
        },
        "AK": {
            "fillKey": "Republican",
            "per": 1
        },
        "AR": {
            "fillKey": "Republican",
            "per": 1
        },
        "AL": {
            "fillKey": "Republican",
            "per": 32
        }
    }
});
election.labels();
